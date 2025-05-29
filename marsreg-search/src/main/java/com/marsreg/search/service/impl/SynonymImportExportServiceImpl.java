package com.marsreg.search.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.marsreg.search.model.SynonymGroup;
import com.marsreg.search.model.SynonymImportExportTask;
import com.marsreg.search.service.SynonymImportExportService;
import com.marsreg.search.service.SynonymService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.marsreg.common.util.StringUtils;
import com.marsreg.common.annotation.Log;
import com.marsreg.common.annotation.RateLimit;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SynonymImportExportServiceImpl implements SynonymImportExportService {

    private final SynonymService synonymService;
    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String EXPORT_DIR = "exports/synonyms";
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("CSV", "JSON", "XML", "EXCEL");
    private static final String TASK_KEY_PREFIX = "synonym:task:";
    private static final String USER_TASKS_KEY_PREFIX = "synonym:user:tasks:";
    private static final int TASK_EXPIRY_DAYS = 7;
    private static final int BATCH_SIZE = 1000;
    
    private final ExecutorService taskExecutor = new ThreadPoolExecutor(
        2, 4, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(100),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );
    
    @Override
    public String exportSynonyms(String format, String category) {
        try {
            // 获取同义词组
            List<SynonymGroup> groups = category != null ?
                synonymService.getSynonymGroupsByCategory(category) :
                synonymService.getAllSynonymGroups();
            
            // 创建导出目录
            Path exportPath = Paths.get(EXPORT_DIR);
            if (!Files.exists(exportPath)) {
                Files.createDirectories(exportPath);
            }
            
            // 生成文件名
            String fileName = String.format("synonyms_%s.%s",
                LocalDateTime.now().format(FILE_NAME_FORMATTER),
                format.toLowerCase());
            Path filePath = exportPath.resolve(fileName);
            
            // 导出数据
            switch (format.toUpperCase()) {
                case "CSV":
                    exportToCsv(groups, filePath);
                    break;
                case "JSON":
                    exportToJson(groups, filePath);
                    break;
                case "XML":
                    exportToXml(groups, filePath);
                    break;
                case "EXCEL":
                    exportToExcel(groups, filePath);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的导出格式: " + format);
            }
            
            return filePath.toString();
        } catch (Exception e) {
            log.error("导出同义词组失败", e);
            throw new RuntimeException("导出同义词组失败", e);
        }
    }
    
    @Override
    public int importSynonyms(String filePath, String format) {
        try {
            List<SynonymGroup> groups;
            
            // 导入数据
            switch (format.toUpperCase()) {
                case "CSV":
                    groups = importFromCsv(filePath);
                    break;
                case "JSON":
                    groups = importFromJson(filePath);
                    break;
                case "XML":
                    groups = importFromXml(filePath);
                    break;
                case "EXCEL":
                    groups = importFromExcel(filePath);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的导入格式: " + format);
            }
            
            // 验证数据
            if (!validateSynonyms(groups)) {
                throw new IllegalArgumentException("同义词组数据验证失败");
            }
            
            // 保存数据
            synonymService.addSynonymGroups(groups);
            
            return groups.size();
        } catch (Exception e) {
            log.error("导入同义词组失败", e);
            throw new RuntimeException("导入同义词组失败", e);
        }
    }
    
    @Override
    public String exportSynonymsToString(List<SynonymGroup> groups, String format) {
        try {
            switch (format.toUpperCase()) {
                case "CSV":
                    return exportToCsvString(groups);
                case "JSON":
                    return exportToJsonString(groups);
                case "XML":
                    return exportToXmlString(groups);
                default:
                    throw new IllegalArgumentException("不支持的导出格式: " + format);
            }
        } catch (Exception e) {
            log.error("导出同义词组到字符串失败", e);
            throw new RuntimeException("导出同义词组到字符串失败", e);
        }
    }
    
    @Override
    public List<SynonymGroup> importSynonymsFromString(String content, String format) {
        try {
            List<SynonymGroup> groups;
            
            switch (format.toUpperCase()) {
                case "CSV":
                    groups = importFromCsvString(content);
                    break;
                case "JSON":
                    groups = importFromJsonString(content);
                    break;
                case "XML":
                    groups = importFromXmlString(content);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的导入格式: " + format);
            }
            
            if (!validateSynonyms(groups)) {
                throw new IllegalArgumentException("同义词组数据验证失败");
            }
            
            return groups;
        } catch (Exception e) {
            log.error("从字符串导入同义词组失败", e);
            throw new RuntimeException("从字符串导入同义词组失败", e);
        }
    }
    
    @Override
    public boolean validateSynonyms(List<SynonymGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return false;
        }
        
        for (SynonymGroup group : groups) {
            // 验证必填字段
            if (group.getTerms() == null || group.getTerms().isEmpty()) {
                return false;
            }
            
            // 验证词不能为空
            if (group.getTerms().stream().anyMatch(term -> StringUtils.isEmpty(term))) {
                return false;
            }
            
            // 验证权重范围
            if (group.getWeight() != null && (group.getWeight() < 0 || group.getWeight() > 1)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public List<String> getSupportedFormats() {
        return SUPPORTED_FORMATS;
    }
    
    @Override
    public String getMediaType(String format) {
        switch (format.toUpperCase()) {
            case "CSV":
                return "text/csv";
            case "JSON":
                return MediaType.APPLICATION_JSON_VALUE;
            case "XML":
                return MediaType.APPLICATION_XML_VALUE;
            case "EXCEL":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default:
                return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
    
    private void exportToCsv(List<SynonymGroup> groups, Path filePath) throws IOException {
        try (Writer writer = new FileWriter(filePath.toFile());
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                 .withHeader("ID", "Terms", "Category", "Weight", "Enabled", "Description"))) {
            
            for (SynonymGroup group : groups) {
                printer.printRecord(
                    group.getId(),
                    String.join(",", group.getTerms()),
                    group.getCategory(),
                    group.getWeight(),
                    group.getEnabled(),
                    group.getDescription()
                );
            }
        }
    }
    
    private void exportToJson(List<SynonymGroup> groups, Path filePath) throws IOException {
        objectMapper.writeValue(filePath.toFile(), groups);
    }
    
    private void exportToXml(List<SynonymGroup> groups, Path filePath) throws IOException {
        xmlMapper.writeValue(filePath.toFile(), groups);
    }
    
    private void exportToExcel(List<SynonymGroup> groups, Path filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Synonyms");
            
            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Terms", "Category", "Weight", "Enabled", "Description"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            // 写入数据
            int rowNum = 1;
            for (SynonymGroup group : groups) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(group.getId());
                row.createCell(1).setCellValue(String.join(",", group.getTerms()));
                row.createCell(2).setCellValue(group.getCategory());
                row.createCell(3).setCellValue(group.getWeight());
                row.createCell(4).setCellValue(group.getEnabled());
                row.createCell(5).setCellValue(group.getDescription());
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 写入文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
                workbook.write(fileOut);
            }
        }
    }
    
    private String exportToCsvString(List<SynonymGroup> groups) throws IOException {
        StringWriter writer = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
            .withHeader("ID", "Terms", "Category", "Weight", "Enabled", "Description"))) {
            
            for (SynonymGroup group : groups) {
                printer.printRecord(
                    group.getId(),
                    String.join(",", group.getTerms()),
                    group.getCategory(),
                    group.getWeight(),
                    group.getEnabled(),
                    group.getDescription()
                );
            }
        }
        return writer.toString();
    }
    
    private String exportToJsonString(List<SynonymGroup> groups) throws IOException {
        return objectMapper.writeValueAsString(groups);
    }
    
    private String exportToXmlString(List<SynonymGroup> groups) throws IOException {
        return xmlMapper.writeValueAsString(groups);
    }
    
    private List<SynonymGroup> importFromCsv(String filePath) throws IOException {
        List<SynonymGroup> groups = new ArrayList<>();
        
        try (Reader reader = new FileReader(filePath)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(reader);
            
            for (CSVRecord record : records) {
                SynonymGroup group = SynonymGroup.builder()
                    .id(record.get("ID"))
                    .terms(Arrays.asList(record.get("Terms").split(",")))
                    .category(record.get("Category"))
                    .weight(Double.parseDouble(record.get("Weight")))
                    .enabled(Boolean.parseBoolean(record.get("Enabled")))
                    .description(record.get("Description"))
                    .build();
                groups.add(group);
            }
        }
        
        return groups;
    }
    
    private List<SynonymGroup> importFromJson(String filePath) throws IOException {
        return objectMapper.readValue(
            new File(filePath),
            objectMapper.getTypeFactory().constructCollectionType(List.class, SynonymGroup.class)
        );
    }
    
    private List<SynonymGroup> importFromXml(String filePath) throws IOException {
        return xmlMapper.readValue(
            new File(filePath),
            xmlMapper.getTypeFactory().constructCollectionType(List.class, SynonymGroup.class)
        );
    }
    
    private List<SynonymGroup> importFromExcel(String filePath) throws IOException {
        List<SynonymGroup> groups = new ArrayList<>();
        
        try (Workbook workbook = WorkbookFactory.create(new File(filePath))) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // 跳过标题行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                SynonymGroup group = SynonymGroup.builder()
                    .id(getCellValueAsString(row.getCell(0)))
                    .terms(Arrays.asList(getCellValueAsString(row.getCell(1)).split(",")))
                    .category(getCellValueAsString(row.getCell(2)))
                    .weight(Double.parseDouble(getCellValueAsString(row.getCell(3))))
                    .enabled(Boolean.parseBoolean(getCellValueAsString(row.getCell(4))))
                    .description(getCellValueAsString(row.getCell(5)))
                    .build();
                groups.add(group);
            }
        }
        
        return groups;
    }
    
    private List<SynonymGroup> importFromCsvString(String content) throws IOException {
        List<SynonymGroup> groups = new ArrayList<>();
        
        try (Reader reader = new StringReader(content)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(reader);
            
            for (CSVRecord record : records) {
                SynonymGroup group = SynonymGroup.builder()
                    .id(record.get("ID"))
                    .terms(Arrays.asList(record.get("Terms").split(",")))
                    .category(record.get("Category"))
                    .weight(Double.parseDouble(record.get("Weight")))
                    .enabled(Boolean.parseBoolean(record.get("Enabled")))
                    .description(record.get("Description"))
                    .build();
                groups.add(group);
            }
        }
        
        return groups;
    }
    
    private List<SynonymGroup> importFromJsonString(String content) throws IOException {
        return objectMapper.readValue(
            content,
            objectMapper.getTypeFactory().constructCollectionType(List.class, SynonymGroup.class)
        );
    }
    
    private List<SynonymGroup> importFromXmlString(String content) throws IOException {
        return xmlMapper.readValue(
            content,
            xmlMapper.getTypeFactory().constructCollectionType(List.class, SynonymGroup.class)
        );
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    @Override
    public String createImportTask(String userId, String filePath, String format) {
        String taskId = UUID.randomUUID().toString();
        SynonymImportExportTask task = SynonymImportExportTask.builder()
            .taskId(taskId)
            .userId(userId)
            .type(SynonymImportExportTask.TaskType.IMPORT)
            .status(SynonymImportExportTask.TaskStatus.PENDING)
            .format(format)
            .filePath(filePath)
            .startTime(LocalDateTime.now())
            .build();
        
        saveTask(task);
        addTaskToUser(userId, taskId);
        
        taskExecutor.submit(() -> processImportTask(task));
        
        return taskId;
    }
    
    @Override
    public String createExportTask(String userId, String format, String category) {
        String taskId = UUID.randomUUID().toString();
        SynonymImportExportTask task = SynonymImportExportTask.builder()
            .taskId(taskId)
            .userId(userId)
            .type(SynonymImportExportTask.TaskType.EXPORT)
            .status(SynonymImportExportTask.TaskStatus.PENDING)
            .format(format)
            .category(category)
            .startTime(LocalDateTime.now())
            .build();
        
        saveTask(task);
        addTaskToUser(userId, taskId);
        
        taskExecutor.submit(() -> processExportTask(task));
        
        return taskId;
    }
    
    @Override
    public SynonymImportExportTask getTaskStatus(String taskId) {
        String taskJson = redisTemplate.opsForValue().get(TASK_KEY_PREFIX + taskId);
        if (taskJson == null) {
            return null;
        }
        try {
            return objectMapper.readValue(taskJson, SynonymImportExportTask.class);
        } catch (Exception e) {
            log.error("Failed to parse task: {}", taskId, e);
            return null;
        }
    }
    
    @Override
    public boolean cancelTask(String taskId) {
        SynonymImportExportTask task = getTaskStatus(taskId);
        if (task == null || task.isCompleted()) {
            return false;
        }
        
        task.setStatus(SynonymImportExportTask.TaskStatus.CANCELLED);
        task.setEndTime(LocalDateTime.now());
        saveTask(task);
        
        return true;
    }
    
    @Override
    public List<SynonymImportExportTask> getUserTasks(String userId) {
        Set<String> taskIds = redisTemplate.opsForSet().members(USER_TASKS_KEY_PREFIX + userId);
        if (taskIds == null) {
            return Collections.emptyList();
        }
        
        return taskIds.stream()
            .map(taskId -> getTaskStatus(taskId))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    @Override
    @Scheduled(cron = "0 0 0 * * ?") // 每天凌晨执行
    public int cleanupTasks() {
        return cleanupTasks(TASK_EXPIRY_DAYS);
    }
    
    @Override
    public int cleanupTasks(int days) {
        LocalDateTime expiryTime = LocalDateTime.now().minusDays(days);
        int count = 0;
        
        Collection<String> userKeys = redisTemplate.keys(USER_TASKS_KEY_PREFIX + "*");
        if (userKeys != null) {
            for (String userId : userKeys) {
                Set<String> taskIds = redisTemplate.opsForSet().members(userId);
                if (taskIds != null) {
                    for (String taskId : taskIds) {
                        SynonymImportExportTask task = getTaskStatus(taskId);
                        if (task != null && task.getEndTime() != null && task.getEndTime().isBefore(expiryTime)) {
                            redisTemplate.delete(TASK_KEY_PREFIX + taskId);
                            redisTemplate.opsForSet().remove(userId, taskId);
                            count++;
                        }
                    }
                }
            }
        }
        
        return count;
    }
    
    private void processImportTask(SynonymImportExportTask task) {
        try {
            updateTaskStatus(task, SynonymImportExportTask.TaskStatus.PROCESSING);
            
            List<SynonymGroup> groups = new ArrayList<>();
            switch (task.getFormat().toUpperCase()) {
                case "CSV":
                    groups = importFromCsv(task.getFilePath());
                    break;
                case "JSON":
                    groups = importFromJson(task.getFilePath());
                    break;
                case "XML":
                    groups = importFromXml(task.getFilePath());
                    break;
                case "EXCEL":
                    groups = importFromExcel(task.getFilePath());
                    break;
            }
            
            task.setTotalCount(groups.size());
            
            // 分批处理
            for (int i = 0; i < groups.size(); i += BATCH_SIZE) {
                if (task.getStatus() == SynonymImportExportTask.TaskStatus.CANCELLED) {
                    break;
                }
                
                int endIndex = Math.min(i + BATCH_SIZE, groups.size());
                List<SynonymGroup> batch = groups.subList(i, endIndex);
                
                try {
                    if (validateSynonyms(batch)) {
                        synonymService.addSynonymGroups(batch);
                        task.setSuccessCount(task.getSuccessCount() + batch.size());
                    } else {
                        task.setFailureCount(task.getFailureCount() + batch.size());
                    }
                } catch (Exception e) {
                    log.error("Failed to process batch: {}", i, e);
                    task.setFailureCount(task.getFailureCount() + batch.size());
                }
                
                task.setProcessedCount(endIndex);
                saveTask(task);
            }
            
            updateTaskStatus(task, SynonymImportExportTask.TaskStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Failed to process import task: {}", task.getTaskId(), e);
            task.setErrorMessage(e.getMessage());
            updateTaskStatus(task, SynonymImportExportTask.TaskStatus.FAILED);
        }
    }
    
    private void processExportTask(SynonymImportExportTask task) {
        try {
            updateTaskStatus(task, SynonymImportExportTask.TaskStatus.PROCESSING);
            
            List<SynonymGroup> groups = task.getCategory() != null ?
                synonymService.getSynonymGroupsByCategory(task.getCategory()) :
                synonymService.getAllSynonymGroups();
            
            task.setTotalCount(groups.size());
            
            // 创建导出目录
            Path exportPath = Paths.get(EXPORT_DIR);
            if (!Files.exists(exportPath)) {
                Files.createDirectories(exportPath);
            }
            
            // 生成文件名
            String fileName = String.format("synonyms_%s.%s",
                LocalDateTime.now().format(FILE_NAME_FORMATTER),
                task.getFormat().toLowerCase());
            Path filePath = exportPath.resolve(fileName);
            task.setFilePath(filePath.toString());
            
            // 分批导出
            switch (task.getFormat().toUpperCase()) {
                case "CSV":
                    exportToCsv(groups, filePath);
                    break;
                case "JSON":
                    exportToJson(groups, filePath);
                    break;
                case "XML":
                    exportToXml(groups, filePath);
                    break;
                case "EXCEL":
                    exportToExcel(groups, filePath);
                    break;
            }
            
            task.setProcessedCount(groups.size());
            task.setSuccessCount(groups.size());
            updateTaskStatus(task, SynonymImportExportTask.TaskStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Failed to process export task: {}", task.getTaskId(), e);
            task.setErrorMessage(e.getMessage());
            updateTaskStatus(task, SynonymImportExportTask.TaskStatus.FAILED);
        }
    }
    
    private void updateTaskStatus(SynonymImportExportTask task, SynonymImportExportTask.TaskStatus status) {
        task.setStatus(status);
        if (status == SynonymImportExportTask.TaskStatus.COMPLETED ||
            status == SynonymImportExportTask.TaskStatus.FAILED ||
            status == SynonymImportExportTask.TaskStatus.CANCELLED) {
            task.setEndTime(LocalDateTime.now());
        }
        saveTask(task);
    }
    
    private void saveTask(SynonymImportExportTask task) {
        try {
            String taskJson = objectMapper.writeValueAsString(task);
            redisTemplate.opsForValue().set(
                TASK_KEY_PREFIX + task.getTaskId(),
                taskJson,
                TASK_EXPIRY_DAYS,
                TimeUnit.DAYS
            );
        } catch (Exception e) {
            log.error("Failed to save task: {}", task.getTaskId(), e);
        }
    }
    
    private void addTaskToUser(String userId, String taskId) {
        redisTemplate.opsForSet().add(USER_TASKS_KEY_PREFIX + userId, taskId);
    }

    @Override
    @Log(module = "同义词管理", operation = "导入", description = "导入同义词")
    @RateLimit(limit = 10, time = 60)
    public void importSynonyms(MultipartFile file) {
        // ... existing code ...
    }

    @Override
    @Log(module = "同义词管理", operation = "导出", description = "导出同义词")
    @RateLimit(limit = 20, time = 60)
    public byte[] exportSynonyms() {
        // ... existing code ...
    }
} 