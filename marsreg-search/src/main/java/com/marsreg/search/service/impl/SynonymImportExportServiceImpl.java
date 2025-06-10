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
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
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
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class SynonymImportExportServiceImpl implements SynonymImportExportService {

    private final SynonymService synonymService;
    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
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
            Path exportPath = Path.of(EXPORT_DIR);
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
                log.warn("Group has no terms");
                return false;
            }
            
            // 验证词不能为空
            if (group.getTerms().stream().anyMatch(term -> StringUtils.isEmpty(term))) {
                log.warn("Group contains empty terms");
                return false;
            }
            
            // 验证词长度
            if (group.getTerms().stream().anyMatch(term -> term.length() > 100)) {
                log.warn("Group contains terms longer than 100 characters");
                return false;
            }
            
            // 验证权重范围
            if (group.getWeight() != null && (group.getWeight() < 0 || group.getWeight() > 1)) {
                log.warn("Group has invalid weight: {}", group.getWeight());
                return false;
            }
            
            // 验证分类长度
            if (group.getCategory() != null && group.getCategory().length() > 50) {
                log.warn("Group has category longer than 50 characters");
                return false;
            }
            
            // 验证描述长度
            if (group.getDescription() != null && group.getDescription().length() > 500) {
                log.warn("Group has description longer than 500 characters");
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
        if (groups == null || groups.isEmpty()) {
            throw new IllegalArgumentException("同义词组列表不能为空");
        }
        
        try (Writer writer = new FileWriter(filePath.toFile(), StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                 .withHeader("ID", "Terms", "Category", "Weight", "Enabled", "Description"))) {
            
            int successCount = 0;
            int failureCount = 0;
            
            for (SynonymGroup group : groups) {
                try {
                    if (group == null || group.getTerms() == null || group.getTerms().isEmpty()) {
                        log.warn("跳过无效的同义词组");
                        failureCount++;
                        continue;
                    }
                    
                    printer.printRecord(
                        group.getId(),
                        String.join(",", group.getTerms()),
                        group.getCategory(),
                        group.getWeight(),
                        group.getEnabled(),
                        group.getDescription()
                    );
                    successCount++;
                } catch (Exception e) {
                    log.error("导出同义词组失败: {}, 错误: {}", group.getId(), e.getMessage());
                    failureCount++;
                }
            }
            
            log.info("CSV导出完成: 成功={}, 失败={}", successCount, failureCount);
        } catch (Exception e) {
            log.error("导出CSV文件失败: {}", filePath, e);
            throw new IOException("导出CSV文件失败", e);
        }
    }
    
    private void exportToJson(List<SynonymGroup> groups, Path filePath) throws IOException {
        if (groups == null || groups.isEmpty()) {
            throw new IllegalArgumentException("同义词组列表不能为空");
        }
        
        try {
            // 验证数据
            List<SynonymGroup> validGroups = groups.stream()
                .filter(group -> group != null && group.getTerms() != null && !group.getTerms().isEmpty())
                .collect(Collectors.toList());
            
            if (validGroups.isEmpty()) {
                throw new IllegalArgumentException("没有有效的同义词组可以导出");
            }
            
            objectMapper.writeValue(filePath.toFile(), validGroups);
            log.info("JSON导出完成: 共{}个同义词组", validGroups.size());
        } catch (Exception e) {
            log.error("导出JSON文件失败: {}", filePath, e);
            throw new IOException("导出JSON文件失败", e);
        }
    }
    
    private void exportToXml(List<SynonymGroup> groups, Path filePath) throws IOException {
        if (groups == null || groups.isEmpty()) {
            throw new IllegalArgumentException("同义词组列表不能为空");
        }
        
        try {
            // 验证数据
            List<SynonymGroup> validGroups = groups.stream()
                .filter(group -> group != null && group.getTerms() != null && !group.getTerms().isEmpty())
                .collect(Collectors.toList());
            
            if (validGroups.isEmpty()) {
                throw new IllegalArgumentException("没有有效的同义词组可以导出");
            }
            
            xmlMapper.writeValue(filePath.toFile(), validGroups);
            log.info("XML导出完成: 共{}个同义词组", validGroups.size());
        } catch (Exception e) {
            log.error("导出XML文件失败: {}", filePath, e);
            throw new IOException("导出XML文件失败", e);
        }
    }
    
    private void exportToExcel(List<SynonymGroup> groups, Path filePath) throws IOException {
        if (groups == null || groups.isEmpty()) {
            throw new IllegalArgumentException("同义词组列表不能为空");
        }
        
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
            int successCount = 0;
            int failureCount = 0;
            
            for (SynonymGroup group : groups) {
                try {
                    if (group == null || group.getTerms() == null || group.getTerms().isEmpty()) {
                        log.warn("跳过无效的同义词组");
                        failureCount++;
                        continue;
                    }
                    
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(group.getId());
                    row.createCell(1).setCellValue(String.join(",", group.getTerms()));
                    row.createCell(2).setCellValue(group.getCategory());
                    row.createCell(3).setCellValue(group.getWeight());
                    row.createCell(4).setCellValue(group.getEnabled());
                    row.createCell(5).setCellValue(group.getDescription());
                    successCount++;
                } catch (Exception e) {
                    log.error("导出同义词组到Excel失败: {}, 错误: {}", group.getId(), e.getMessage());
                    failureCount++;
                }
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 写入文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
                workbook.write(fileOut);
            }
            
            log.info("Excel导出完成: 成功={}, 失败={}", successCount, failureCount);
        } catch (Exception e) {
            log.error("导出Excel文件失败: {}", filePath, e);
            throw new IOException("导出Excel文件失败", e);
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
        if (!Files.exists(Paths.get(filePath))) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }
        
        List<SynonymGroup> groups = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        try (Reader reader = new FileReader(filePath, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(reader);
            
            for (CSVRecord record : records) {
                try {
                    if (record == null) {
                        log.warn("跳过空记录");
                        failureCount++;
                        continue;
                    }
                    
                    SynonymGroup group = SynonymGroup.builder()
                        .id(record.get("ID"))
                        .terms(Arrays.asList(record.get("Terms").split(",")))
                        .category(record.get("Category"))
                        .weight(Double.parseDouble(record.get("Weight")))
                        .enabled(Boolean.parseBoolean(record.get("Enabled")))
                        .description(record.get("Description"))
                        .build();
                    
                    if (validateSynonyms(Collections.singletonList(group))) {
                        groups.add(group);
                        successCount++;
                    } else {
                        log.warn("同义词组验证失败: {}", group.getId());
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.error("解析CSV记录失败: {}, 错误: {}", record, e.getMessage());
                    failureCount++;
                }
            }
        }
        
        log.info("CSV导入完成: 成功={}, 失败={}", successCount, failureCount);
        return groups;
    }
    
    private List<SynonymGroup> importFromJson(String filePath) throws IOException {
        if (!Files.exists(Paths.get(filePath))) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }
        
        try {
            List<SynonymGroup> groups = objectMapper.readValue(
                new File(filePath),
                objectMapper.getTypeFactory().constructCollectionType(List.class, SynonymGroup.class)
            );
            
            if (groups == null || groups.isEmpty()) {
                log.warn("JSON文件中没有同义词组数据");
                return Collections.emptyList();
            }
            
            final int[] successCount = {0};
            final int[] failureCount = {0};
            
            List<SynonymGroup> validGroups = groups.stream()
                .filter(group -> {
                    try {
                        if (group == null) {
                            log.warn("跳过空同义词组");
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证必填字段
                        if (group.getTerms() == null || group.getTerms().isEmpty()) {
                            log.warn("同义词组缺少词条: {}", group.getId());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证词条
                        for (String term : group.getTerms()) {
                            if (StringUtils.isEmpty(term)) {
                                log.warn("同义词组包含空词条: {}", group.getId());
                                failureCount[0]++;
                                return false;
                            }
                            if (term.length() > 100) {
                                log.warn("同义词组包含超长词条: {}, 长度: {}", term, term.length());
                                failureCount[0]++;
                                return false;
                            }
                        }
                        
                        // 验证ID
                        if (group.getId() != null && group.getId().length() > 50) {
                            log.warn("同义词组ID超长: {}, 长度: {}", group.getId(), group.getId().length());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证分类
                        if (group.getCategory() != null && group.getCategory().length() > 50) {
                            log.warn("同义词组分类超长: {}, 长度: {}", group.getCategory(), group.getCategory().length());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证描述
                        if (group.getDescription() != null && group.getDescription().length() > 500) {
                            log.warn("同义词组描述超长: {}, 长度: {}", group.getDescription(), group.getDescription().length());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证权重
                        if (group.getWeight() != null) {
                            if (group.getWeight() < 0 || group.getWeight() > 1) {
                                log.warn("同义词组权重超出范围: {}, 值: {}", group.getId(), group.getWeight());
                                failureCount[0]++;
                                return false;
                            }
                        }
                        
                        // 验证词条重复
                        Set<String> uniqueTerms = new HashSet<>(group.getTerms());
                        if (uniqueTerms.size() != group.getTerms().size()) {
                            log.warn("同义词组包含重复词条: {}", group.getId());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证词条格式
                        for (String term : group.getTerms()) {
                            if (!term.matches("^[\\w\\u4e00-\\u9fa5\\s\\-]+$")) {
                                log.warn("同义词组包含非法字符: {}, 词条: {}", group.getId(), term);
                                failureCount[0]++;
                                return false;
                            }
                        }
                        
                        boolean isValid = validateSynonyms(Collections.singletonList(group));
                        if (isValid) {
                            successCount[0]++;
                        } else {
                            failureCount[0]++;
                        }
                        return isValid;
                    } catch (Exception e) {
                        log.error("验证同义词组失败: {}, 错误: {}", group.getId(), e.getMessage());
                        failureCount[0]++;
                        return false;
                    }
                })
                .collect(Collectors.toList());
            
            log.info("JSON导入完成: 成功={}, 失败={}", successCount[0], failureCount[0]);
            return validGroups;
        } catch (Exception e) {
            log.error("解析JSON文件失败: {}", filePath, e);
            throw new IOException("解析JSON文件失败: " + e.getMessage(), e);
        }
    }
    
    private List<SynonymGroup> importFromXml(String filePath) throws IOException {
        if (!Files.exists(Paths.get(filePath))) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }
        
        try {
            List<SynonymGroup> groups = xmlMapper.readValue(
                new File(filePath),
                xmlMapper.getTypeFactory().constructCollectionType(List.class, SynonymGroup.class)
            );
            
            if (groups == null || groups.isEmpty()) {
                log.warn("XML文件中没有同义词组数据");
                return Collections.emptyList();
            }
            
            final int[] successCount = {0};
            final int[] failureCount = {0};
            
            List<SynonymGroup> validGroups = groups.stream()
                .filter(group -> {
                    try {
                        if (group == null) {
                            log.warn("跳过空同义词组");
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证必填字段
                        if (group.getTerms() == null || group.getTerms().isEmpty()) {
                            log.warn("同义词组缺少词条: {}", group.getId());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证词条
                        for (String term : group.getTerms()) {
                            if (StringUtils.isEmpty(term)) {
                                log.warn("同义词组包含空词条: {}", group.getId());
                                failureCount[0]++;
                                return false;
                            }
                            if (term.length() > 100) {
                                log.warn("同义词组包含超长词条: {}, 长度: {}", term, term.length());
                                failureCount[0]++;
                                return false;
                            }
                        }
                        
                        // 验证ID
                        if (group.getId() != null && group.getId().length() > 50) {
                            log.warn("同义词组ID超长: {}, 长度: {}", group.getId(), group.getId().length());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证分类
                        if (group.getCategory() != null && group.getCategory().length() > 50) {
                            log.warn("同义词组分类超长: {}, 长度: {}", group.getCategory(), group.getCategory().length());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证描述
                        if (group.getDescription() != null && group.getDescription().length() > 500) {
                            log.warn("同义词组描述超长: {}, 长度: {}", group.getDescription(), group.getDescription().length());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证权重
                        if (group.getWeight() != null) {
                            if (group.getWeight() < 0 || group.getWeight() > 1) {
                                log.warn("同义词组权重超出范围: {}, 值: {}", group.getId(), group.getWeight());
                                failureCount[0]++;
                                return false;
                            }
                        }
                        
                        // 验证词条重复
                        Set<String> uniqueTerms = new HashSet<>(group.getTerms());
                        if (uniqueTerms.size() != group.getTerms().size()) {
                            log.warn("同义词组包含重复词条: {}", group.getId());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证词条格式
                        for (String term : group.getTerms()) {
                            if (!term.matches("^[\\w\\u4e00-\\u9fa5\\s\\-]+$")) {
                                log.warn("同义词组包含非法字符: {}, 词条: {}", group.getId(), term);
                                failureCount[0]++;
                                return false;
                            }
                        }
                        
                        boolean isValid = validateSynonyms(Collections.singletonList(group));
                        if (isValid) {
                            successCount[0]++;
                        } else {
                            failureCount[0]++;
                        }
                        return isValid;
                    } catch (Exception e) {
                        log.error("验证同义词组失败: {}, 错误: {}", group.getId(), e.getMessage());
                        failureCount[0]++;
                        return false;
                    }
                })
                .collect(Collectors.toList());
            
            log.info("XML导入完成: 成功={}, 失败={}", successCount[0], failureCount[0]);
            return validGroups;
        } catch (Exception e) {
            log.error("解析XML文件失败: {}", filePath, e);
            throw new IOException("解析XML文件失败: " + e.getMessage(), e);
        }
    }
    
    private List<SynonymGroup> importFromExcel(String filePath) throws IOException {
        if (!Files.exists(Paths.get(filePath))) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }
        
        List<SynonymGroup> groups = new ArrayList<>();
        final int[] successCount = {0};
        final int[] failureCount = {0};
        
        try (Workbook workbook = WorkbookFactory.create(new File(filePath))) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IOException("Excel文件中没有工作表");
            }
            
            // 验证标题行
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("Excel文件缺少标题行");
            }
            
            String[] expectedHeaders = {"ID", "Terms", "Category", "Weight", "Enabled", "Description"};
            for (int i = 0; i < expectedHeaders.length; i++) {
                Cell cell = headerRow.getCell(i);
                if (cell == null || !expectedHeaders[i].equals(getCellValueAsString(cell))) {
                    throw new IOException("Excel文件标题行格式不正确，期望: " + String.join(", ", expectedHeaders));
                }
            }
            
            // 跳过标题行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    log.warn("跳过空行: {}", i);
                    failureCount[0]++;
                    continue;
                }
                
                try {
                    String id = getCellValueAsString(row.getCell(0));
                    String termsStr = getCellValueAsString(row.getCell(1));
                    String category = getCellValueAsString(row.getCell(2));
                    String weightStr = getCellValueAsString(row.getCell(3));
                    String enabledStr = getCellValueAsString(row.getCell(4));
                    String description = getCellValueAsString(row.getCell(5));
                    
                    if (StringUtils.isEmpty(termsStr)) {
                        log.warn("第{}行没有同义词数据", i);
                        failureCount[0]++;
                        continue;
                    }
                    
                    // 验证数据
                    if (id != null && id.length() > 50) {
                        log.warn("第{}行ID长度超过50个字符", i);
                        failureCount[0]++;
                        continue;
                    }
                    
                    if (category != null && category.length() > 50) {
                        log.warn("第{}行分类长度超过50个字符", i);
                        failureCount[0]++;
                        continue;
                    }
                    
                    if (description != null && description.length() > 500) {
                        log.warn("第{}行描述长度超过500个字符", i);
                        failureCount[0]++;
                        continue;
                    }
                    
                    // 验证权重
                    Double weight = null;
                    if (!StringUtils.isEmpty(weightStr)) {
                        try {
                            weight = Double.parseDouble(weightStr);
                            if (weight < 0 || weight > 1) {
                                log.warn("第{}行权重值必须在0-1之间", i);
                                failureCount[0]++;
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            log.warn("第{}行权重格式不正确", i);
                            failureCount[0]++;
                            continue;
                        }
                    }
                    
                    // 验证启用状态
                    Boolean enabled = true;
                    if (!StringUtils.isEmpty(enabledStr)) {
                        try {
                            enabled = Boolean.parseBoolean(enabledStr);
                        } catch (Exception e) {
                            log.warn("第{}行启用状态格式不正确", i);
                            failureCount[0]++;
                            continue;
                        }
                    }
                    
                    SynonymGroup group = SynonymGroup.builder()
                        .id(id)
                        .terms(Arrays.asList(termsStr.split(",")))
                        .category(category)
                        .weight(weight)
                        .enabled(enabled)
                        .description(description)
                        .build();
                    
                    if (validateSynonyms(Collections.singletonList(group))) {
                        groups.add(group);
                        successCount[0]++;
                    } else {
                        log.warn("第{}行同义词组验证失败", i);
                        failureCount[0]++;
                    }
                } catch (Exception e) {
                    log.error("解析Excel行失败: {}, 错误: {}", i, e.getMessage());
                    failureCount[0]++;
                }
            }
            
            log.info("Excel导入完成: 成功={}, 失败={}", successCount[0], failureCount[0]);
            return groups;
        } catch (Exception e) {
            log.error("解析Excel文件失败: {}", filePath, e);
            throw new IOException("解析Excel文件失败: " + e.getMessage(), e);
        }
    }
    
    private List<SynonymGroup> importFromCsvString(String content) throws IOException {
        if (StringUtils.isEmpty(content)) {
            throw new IllegalArgumentException("CSV内容不能为空");
        }
        
        List<SynonymGroup> groups = new ArrayList<>();
        final int[] successCount = {0};
        final int[] failureCount = {0};
        
        try (Reader reader = new StringReader(content)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(reader);
            
            // 验证标题行
            String[] expectedHeaders = {"ID", "Terms", "Category", "Weight", "Enabled", "Description"};
            boolean hasValidHeaders = false;
            for (CSVRecord record : records) {
                for (String header : expectedHeaders) {
                    if (!record.isMapped(header)) {
                        throw new IOException("CSV文件标题行格式不正确，期望: " + String.join(", ", expectedHeaders));
                    }
                }
                hasValidHeaders = true;
                break;
            }
            
            if (!hasValidHeaders) {
                throw new IOException("CSV文件缺少标题行");
            }
            
            for (CSVRecord record : records) {
                try {
                    if (record == null) {
                        log.warn("跳过空记录");
                        failureCount[0]++;
                        continue;
                    }
                    
                    String id = record.get("ID");
                    String termsStr = record.get("Terms");
                    String category = record.get("Category");
                    String weightStr = record.get("Weight");
                    String enabledStr = record.get("Enabled");
                    String description = record.get("Description");
                    
                    if (StringUtils.isEmpty(termsStr)) {
                        log.warn("记录没有同义词数据");
                        failureCount[0]++;
                        continue;
                    }
                    
                    // 验证数据
                    if (id != null && id.length() > 50) {
                        log.warn("ID长度超过50个字符");
                        failureCount[0]++;
                        continue;
                    }
                    
                    if (category != null && category.length() > 50) {
                        log.warn("分类长度超过50个字符");
                        failureCount[0]++;
                        continue;
                    }
                    
                    if (description != null && description.length() > 500) {
                        log.warn("描述长度超过500个字符");
                        failureCount[0]++;
                        continue;
                    }
                    
                    // 验证权重
                    Double weight = null;
                    if (!StringUtils.isEmpty(weightStr)) {
                        try {
                            weight = Double.parseDouble(weightStr);
                            if (weight < 0 || weight > 1) {
                                log.warn("权重值必须在0-1之间");
                                failureCount[0]++;
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            log.warn("权重格式不正确");
                            failureCount[0]++;
                            continue;
                        }
                    }
                    
                    // 验证启用状态
                    Boolean enabled = true;
                    if (!StringUtils.isEmpty(enabledStr)) {
                        try {
                            enabled = Boolean.parseBoolean(enabledStr);
                        } catch (Exception e) {
                            log.warn("启用状态格式不正确");
                            failureCount[0]++;
                            continue;
                        }
                    }
                    
                    SynonymGroup group = SynonymGroup.builder()
                        .id(id)
                        .terms(Arrays.asList(termsStr.split(",")))
                        .category(category)
                        .weight(weight)
                        .enabled(enabled)
                        .description(description)
                        .build();
                    
                    if (validateSynonyms(Collections.singletonList(group))) {
                        groups.add(group);
                        successCount[0]++;
                    } else {
                        log.warn("同义词组验证失败: {}", group.getId());
                        failureCount[0]++;
                    }
                } catch (Exception e) {
                    log.error("解析CSV记录失败: {}, 错误: {}", record, e.getMessage());
                    failureCount[0]++;
                }
            }
        }
        
        log.info("CSV字符串导入完成: 成功={}, 失败={}", successCount[0], failureCount[0]);
        return groups;
    }
    
    private List<SynonymGroup> importFromJsonString(String content) throws IOException {
        if (StringUtils.isEmpty(content)) {
            throw new IllegalArgumentException("JSON内容不能为空");
        }
        
        try {
            List<SynonymGroup> groups = objectMapper.readValue(
                content,
                objectMapper.getTypeFactory().constructCollectionType(List.class, SynonymGroup.class)
            );
            
            if (groups == null || groups.isEmpty()) {
                log.warn("JSON字符串中没有同义词组数据");
                return Collections.emptyList();
            }
            
            final int[] successCount = {0};
            final int[] failureCount = {0};
            
            List<SynonymGroup> validGroups = groups.stream()
                .filter(group -> {
                    try {
                        if (group == null) {
                            log.warn("跳过空同义词组");
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证必填字段
                        if (group.getTerms() == null || group.getTerms().isEmpty()) {
                            log.warn("同义词组缺少词条: {}", group.getId());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证词条
                        for (String term : group.getTerms()) {
                            if (StringUtils.isEmpty(term)) {
                                log.warn("同义词组包含空词条: {}", group.getId());
                                failureCount[0]++;
                                return false;
                            }
                            if (term.length() > 100) {
                                log.warn("同义词组包含超长词条: {}, 长度: {}", term, term.length());
                                failureCount[0]++;
                                return false;
                            }
                        }
                        
                        // 验证ID
                        if (group.getId() != null && group.getId().length() > 50) {
                            log.warn("同义词组ID超长: {}, 长度: {}", group.getId(), group.getId().length());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证分类
                        if (group.getCategory() != null && group.getCategory().length() > 50) {
                            log.warn("同义词组分类超长: {}, 长度: {}", group.getCategory(), group.getCategory().length());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证描述
                        if (group.getDescription() != null && group.getDescription().length() > 500) {
                            log.warn("同义词组描述超长: {}, 长度: {}", group.getDescription(), group.getDescription().length());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证权重
                        if (group.getWeight() != null) {
                            if (group.getWeight() < 0 || group.getWeight() > 1) {
                                log.warn("同义词组权重超出范围: {}, 值: {}", group.getId(), group.getWeight());
                                failureCount[0]++;
                                return false;
                            }
                        }
                        
                        // 验证词条重复
                        Set<String> uniqueTerms = new HashSet<>(group.getTerms());
                        if (uniqueTerms.size() != group.getTerms().size()) {
                            log.warn("同义词组包含重复词条: {}", group.getId());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证词条格式
                        for (String term : group.getTerms()) {
                            if (!term.matches("^[\\w\\u4e00-\\u9fa5\\s\\-]+$")) {
                                log.warn("同义词组包含非法字符: {}, 词条: {}", group.getId(), term);
                                failureCount[0]++;
                                return false;
                            }
                        }
                        
                        boolean isValid = validateSynonyms(Collections.singletonList(group));
                        if (isValid) {
                            successCount[0]++;
                        } else {
                            failureCount[0]++;
                        }
                        return isValid;
                    } catch (Exception e) {
                        log.error("验证同义词组失败: {}, 错误: {}", group.getId(), e.getMessage());
                        failureCount[0]++;
                        return false;
                    }
                })
                .collect(Collectors.toList());
            
            log.info("JSON字符串导入完成: 成功={}, 失败={}", successCount[0], failureCount[0]);
            return validGroups;
        } catch (Exception e) {
            log.error("解析JSON字符串失败: {}", e.getMessage());
            throw new IOException("解析JSON字符串失败: " + e.getMessage(), e);
        }
    }
    
    private List<SynonymGroup> importFromXmlString(String content) throws IOException {
        if (StringUtils.isEmpty(content)) {
            throw new IllegalArgumentException("XML内容不能为空");
        }
        
        try {
            List<SynonymGroup> groups = xmlMapper.readValue(
                content,
                xmlMapper.getTypeFactory().constructCollectionType(List.class, SynonymGroup.class)
            );
            
            if (groups == null || groups.isEmpty()) {
                log.warn("XML字符串中没有同义词组数据");
                return Collections.emptyList();
            }
            
            final int[] successCount = {0};
            final int[] failureCount = {0};
            
            List<SynonymGroup> validGroups = groups.stream()
                .filter(group -> {
                    try {
                        if (group == null) {
                            log.warn("跳过空同义词组");
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证必填字段
                        if (group.getTerms() == null || group.getTerms().isEmpty()) {
                            log.warn("同义词组缺少词条: {}", group.getId());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证词条
                        for (String term : group.getTerms()) {
                            if (StringUtils.isEmpty(term)) {
                                log.warn("同义词组包含空词条: {}", group.getId());
                                failureCount[0]++;
                                return false;
                            }
                            if (term.length() > 100) {
                                log.warn("同义词组包含超长词条: {}, 长度: {}", term, term.length());
                                failureCount[0]++;
                                return false;
                            }
                        }
                        
                        // 验证ID
                        if (group.getId() != null && group.getId().length() > 50) {
                            log.warn("同义词组ID超长: {}, 长度: {}", group.getId(), group.getId().length());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证分类
                        if (group.getCategory() != null && group.getCategory().length() > 50) {
                            log.warn("同义词组分类超长: {}, 长度: {}", group.getCategory(), group.getCategory().length());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证描述
                        if (group.getDescription() != null && group.getDescription().length() > 500) {
                            log.warn("同义词组描述超长: {}, 长度: {}", group.getDescription(), group.getDescription().length());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证权重
                        if (group.getWeight() != null) {
                            if (group.getWeight() < 0 || group.getWeight() > 1) {
                                log.warn("同义词组权重超出范围: {}, 值: {}", group.getId(), group.getWeight());
                                failureCount[0]++;
                                return false;
                            }
                        }
                        
                        // 验证词条重复
                        Set<String> uniqueTerms = new HashSet<>(group.getTerms());
                        if (uniqueTerms.size() != group.getTerms().size()) {
                            log.warn("同义词组包含重复词条: {}", group.getId());
                            failureCount[0]++;
                            return false;
                        }
                        
                        // 验证词条格式
                        for (String term : group.getTerms()) {
                            if (!term.matches("^[\\w\\u4e00-\\u9fa5\\s\\-]+$")) {
                                log.warn("同义词组包含非法字符: {}, 词条: {}", group.getId(), term);
                                failureCount[0]++;
                                return false;
                            }
                        }
                        
                        boolean isValid = validateSynonyms(Collections.singletonList(group));
                        if (isValid) {
                            successCount[0]++;
                        } else {
                            failureCount[0]++;
                        }
                        return isValid;
                    } catch (Exception e) {
                        log.error("验证同义词组失败: {}, 错误: {}", group.getId(), e.getMessage());
                        failureCount[0]++;
                        return false;
                    }
                })
                .collect(Collectors.toList());
            
            log.info("XML字符串导入完成: 成功={}, 失败={}", successCount[0], failureCount[0]);
            return validGroups;
        } catch (Exception e) {
            log.error("解析XML字符串失败: {}", e.getMessage());
            throw new IOException("解析XML字符串失败: " + e.getMessage(), e);
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        try {
            switch (cell.getCellType()) {
                case STRING:
                    String value = cell.getStringCellValue();
                    return value != null ? value.trim() : "";
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getLocalDateTimeCellValue().toString();
                    }
                    double numericValue = cell.getNumericCellValue();
                    // 处理整数和小数
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    }
                    return String.valueOf(numericValue);
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception e) {
                        try {
                            return String.valueOf(cell.getNumericCellValue());
                        } catch (Exception ex) {
                            return cell.getCellFormula();
                        }
                    }
                case BLANK:
                    return "";
                case ERROR:
                    return "#ERROR";
                default:
                    return "";
            }
        } catch (Exception e) {
            log.warn("获取单元格值失败: {}", e.getMessage());
            return "";
        }
    }
    
    @Override
    public String createImportTask(String userId, String filePath, String format) {
        if (StringUtils.isEmpty(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (StringUtils.isEmpty(filePath)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        if (StringUtils.isEmpty(format)) {
            throw new IllegalArgumentException("文件格式不能为空");
        }
        if (!SUPPORTED_FORMATS.contains(format.toUpperCase())) {
            throw new IllegalArgumentException("不支持的导入格式: " + format);
        }
        
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
        
        try {
            saveTask(task);
            addTaskToUser(userId, taskId);
            taskExecutor.submit(() -> processImportTask(task));
            log.info("创建导入任务成功: {}, 用户: {}, 格式: {}", taskId, userId, format);
            return taskId;
        } catch (Exception e) {
            log.error("创建导入任务失败: {}, 用户: {}", taskId, userId, e);
            throw new RuntimeException("创建导入任务失败", e);
        }
    }
    
    @Override
    public String createExportTask(String userId, String format, String category) {
        if (StringUtils.isEmpty(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (StringUtils.isEmpty(format)) {
            throw new IllegalArgumentException("文件格式不能为空");
        }
        if (!SUPPORTED_FORMATS.contains(format.toUpperCase())) {
            throw new IllegalArgumentException("不支持的导出格式: " + format);
        }
        
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
        
        try {
            saveTask(task);
            addTaskToUser(userId, taskId);
            taskExecutor.submit(() -> processExportTask(task));
            log.info("创建导出任务成功: {}, 用户: {}, 格式: {}, 分类: {}", taskId, userId, format, category);
            return taskId;
        } catch (Exception e) {
            log.error("创建导出任务失败: {}, 用户: {}", taskId, userId, e);
            throw new RuntimeException("创建导出任务失败", e);
        }
    }
    
    @Override
    public SynonymImportExportTask getTaskStatus(String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        
        try {
            Object taskJsonObj = redisTemplate.opsForValue().get(TASK_KEY_PREFIX + taskId);
            if (taskJsonObj == null) {
                log.debug("任务不存在: {}", taskId);
                return null;
            }
            
            String taskJson = taskJsonObj.toString();
            SynonymImportExportTask task = objectMapper.readValue(taskJson, SynonymImportExportTask.class);
            log.debug("获取任务状态成功: {}, 状态: {}", taskId, task.getStatus());
            return task;
        } catch (Exception e) {
            log.error("获取任务状态失败: {}", taskId, e);
            return null;
        }
    }
    
    @Override
    public boolean cancelTask(String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        
        try {
            SynonymImportExportTask task = getTaskStatus(taskId);
            if (task == null) {
                log.warn("取消任务失败，任务不存在: {}", taskId);
                return false;
            }
            
            if (task.isCompleted()) {
                log.warn("取消任务失败，任务已完成: {}", taskId);
                return false;
            }
            
            task.setStatus(SynonymImportExportTask.TaskStatus.CANCELLED);
            task.setEndTime(LocalDateTime.now());
            saveTask(task);
            log.info("取消任务成功: {}", taskId);
            return true;
        } catch (Exception e) {
            log.error("取消任务失败: {}", taskId, e);
            return false;
        }
    }
    
    @Override
    public List<SynonymImportExportTask> getUserTasks(String userId) {
        if (StringUtils.isEmpty(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        try {
            String userKey = USER_TASKS_KEY_PREFIX + userId;
            Set<Object> taskIdObjs = redisTemplate.opsForSet().members(userKey);
            if (taskIdObjs == null || taskIdObjs.isEmpty()) {
                log.debug("用户没有任务: {}", userId);
                return Collections.emptyList();
            }
            
            Set<String> taskIds = taskIdObjs.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
            
            List<SynonymImportExportTask> tasks = taskIds.stream()
                .map(this::getTaskStatus)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            log.debug("获取用户任务成功: {}, 任务数: {}", userId, tasks.size());
            return tasks;
        } catch (Exception e) {
            log.error("获取用户任务失败: {}", userId, e);
            return Collections.emptyList();
        }
    }
    
    @Scheduled(cron = "0 0 0 * * ?")
    public int cleanupTasks() {
        return cleanupTasks(TASK_EXPIRY_DAYS);
    }
    
    public int cleanupTasks(int days) {
        LocalDateTime expiryTime = LocalDateTime.now().minusDays(days);
        int count = 0;
        
        try {
            // 使用 SCAN 命令替代 KEYS
            String pattern = USER_TASKS_KEY_PREFIX + "*";
            ScanOptions options = ScanOptions.scanOptions().match(pattern).build();
            
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    String userId = cursor.next();
                    Set<Object> taskIdObjs = redisTemplate.opsForSet().members(userId);
                    if (taskIdObjs != null) {
                        Set<String> taskIds = taskIdObjs.stream()
                            .map(Object::toString)
                            .collect(Collectors.toSet());
                        
                        for (String taskId : taskIds) {
                            try {
                                SynonymImportExportTask task = getTaskStatus(taskId);
                                if (task != null && task.getEndTime() != null && task.getEndTime().isBefore(expiryTime)) {
                                    redisTemplate.delete(TASK_KEY_PREFIX + taskId);
                                    redisTemplate.opsForSet().remove(userId, taskId);
                                    count++;
                                    log.debug("清理过期任务: {}", taskId);
                                }
                            } catch (Exception e) {
                                log.error("清理任务失败: {}", taskId, e);
                            }
                        }
                    }
                }
            }
            
            log.info("任务清理完成，共清理{}个过期任务", count);
        } catch (Exception e) {
            log.error("扫描任务失败", e);
        }
        
        return count;
    }
    
    private void processImportTask(SynonymImportExportTask task) {
        try {
            updateTaskStatus(task, SynonymImportExportTask.TaskStatus.PROCESSING);
            log.info("开始处理导入任务: {}", task.getTaskId());
            
            List<SynonymGroup> groups = new ArrayList<>();
            try {
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
                    default:
                        throw new IllegalArgumentException("不支持的导入格式: " + task.getFormat());
                }
            } catch (Exception e) {
                log.error("读取文件失败: {}", task.getFilePath(), e);
                task.setErrorMessage("读取文件失败: " + e.getMessage());
                updateTaskStatus(task, SynonymImportExportTask.TaskStatus.FAILED);
                return;
            }
            
            if (groups.isEmpty()) {
                log.warn("未找到有效的同义词组，任务: {}", task.getTaskId());
                task.setErrorMessage("未找到有效的同义词组");
                updateTaskStatus(task, SynonymImportExportTask.TaskStatus.FAILED);
                return;
            }
            
            task.setTotalCount(groups.size());
            log.info("找到{}个同义词组，任务: {}", groups.size(), task.getTaskId());
            
            // 分批处理
            for (int i = 0; i < groups.size(); i += BATCH_SIZE) {
                if (task.getStatus() == SynonymImportExportTask.TaskStatus.CANCELLED) {
                    log.info("任务已取消: {}", task.getTaskId());
                    break;
                }
                
                int endIndex = Math.min(i + BATCH_SIZE, groups.size());
                List<SynonymGroup> batch = groups.subList(i, endIndex);
                
                try {
                    if (validateSynonyms(batch)) {
                        synonymService.addSynonymGroups(batch);
                        task.setSuccessCount(task.getSuccessCount() + batch.size());
                        log.debug("成功导入批次 {}-{}, 任务: {}", i, endIndex, task.getTaskId());
                    } else {
                        task.setFailureCount(task.getFailureCount() + batch.size());
                        log.warn("批次 {}-{} 验证失败, 任务: {}", i, endIndex, task.getTaskId());
                    }
                } catch (Exception e) {
                    log.error("处理批次 {}-{} 失败, 任务: {}", i, endIndex, task.getTaskId(), e);
                    task.setFailureCount(task.getFailureCount() + batch.size());
                }
                
                task.setProcessedCount(endIndex);
                saveTask(task);
            }
            
            if (task.getSuccessCount() > 0) {
                updateTaskStatus(task, SynonymImportExportTask.TaskStatus.COMPLETED);
                log.info("导入任务完成: {}, 成功: {}, 失败: {}", 
                    task.getTaskId(), task.getSuccessCount(), task.getFailureCount());
            } else {
                task.setErrorMessage("所有批次导入失败");
                updateTaskStatus(task, SynonymImportExportTask.TaskStatus.FAILED);
                log.error("导入任务失败: {}, 所有批次导入失败", task.getTaskId());
            }
        } catch (Exception e) {
            log.error("处理导入任务失败: {}", task.getTaskId(), e);
            task.setErrorMessage("处理任务失败: " + e.getMessage());
            updateTaskStatus(task, SynonymImportExportTask.TaskStatus.FAILED);
        }
    }
    
    private void processExportTask(SynonymImportExportTask task) {
        try {
            updateTaskStatus(task, SynonymImportExportTask.TaskStatus.PROCESSING);
            log.info("开始处理导出任务: {}", task.getTaskId());
            
            // 获取同义词组
            List<SynonymGroup> groups;
            try {
                groups = task.getCategory() != null ?
                    synonymService.getSynonymGroupsByCategory(task.getCategory()) :
                    synonymService.getAllSynonymGroups();
                
                if (groups.isEmpty()) {
                    log.warn("未找到同义词组，任务: {}", task.getTaskId());
                    task.setErrorMessage("未找到同义词组");
                    updateTaskStatus(task, SynonymImportExportTask.TaskStatus.FAILED);
                    return;
                }
            } catch (Exception e) {
                log.error("获取同义词组失败，任务: {}", task.getTaskId(), e);
                task.setErrorMessage("获取同义词组失败: " + e.getMessage());
                updateTaskStatus(task, SynonymImportExportTask.TaskStatus.FAILED);
                return;
            }
            
            task.setTotalCount(groups.size());
            log.info("找到{}个同义词组，任务: {}", groups.size(), task.getTaskId());
            
            // 创建导出目录
            Path exportPath = Path.of(EXPORT_DIR);
            try {
                if (!Files.exists(exportPath)) {
                    Files.createDirectories(exportPath);
                }
            } catch (Exception e) {
                log.error("创建导出目录失败，任务: {}", task.getTaskId(), e);
                task.setErrorMessage("创建导出目录失败: " + e.getMessage());
                updateTaskStatus(task, SynonymImportExportTask.TaskStatus.FAILED);
                return;
            }
            
            // 生成文件名
            String fileName = String.format("synonyms_%s.%s",
                LocalDateTime.now().format(FILE_NAME_FORMATTER),
                task.getFormat().toLowerCase());
            Path filePath = exportPath.resolve(fileName);
            task.setFilePath(filePath.toString());
            
            // 分批导出
            try {
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
                    default:
                        throw new IllegalArgumentException("不支持的导出格式: " + task.getFormat());
                }
                
                task.setProcessedCount(groups.size());
                task.setSuccessCount(groups.size());
                updateTaskStatus(task, SynonymImportExportTask.TaskStatus.COMPLETED);
                log.info("导出任务完成: {}", task.getTaskId());
            } catch (Exception e) {
                log.error("导出文件失败，任务: {}", task.getTaskId(), e);
                task.setErrorMessage("导出文件失败: " + e.getMessage());
                updateTaskStatus(task, SynonymImportExportTask.TaskStatus.FAILED);
            }
        } catch (Exception e) {
            log.error("处理导出任务失败: {}", task.getTaskId(), e);
            task.setErrorMessage("处理任务失败: " + e.getMessage());
            updateTaskStatus(task, SynonymImportExportTask.TaskStatus.FAILED);
        }
    }
    
    private void updateTaskStatus(SynonymImportExportTask task, SynonymImportExportTask.TaskStatus status) {
        try {
            task.setStatus(status);
            if (status == SynonymImportExportTask.TaskStatus.COMPLETED ||
                status == SynonymImportExportTask.TaskStatus.FAILED ||
                status == SynonymImportExportTask.TaskStatus.CANCELLED) {
                task.setEndTime(LocalDateTime.now());
            }
            saveTask(task);
            log.debug("更新任务状态: {} -> {}, 任务: {}", task.getStatus(), status, task.getTaskId());
        } catch (Exception e) {
            log.error("更新任务状态失败: {}", task.getTaskId(), e);
        }
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
            log.debug("保存任务成功: {}", task.getTaskId());
        } catch (Exception e) {
            log.error("保存任务失败: {}", task.getTaskId(), e);
            throw new RuntimeException("保存任务失败", e);
        }
    }
    
    private void addTaskToUser(String userId, String taskId) {
        try {
            String userKey = USER_TASKS_KEY_PREFIX + userId;
            redisTemplate.opsForSet().add(userKey, taskId);
            log.debug("添加任务到用户: {}, 任务: {}", userId, taskId);
        } catch (Exception e) {
            log.error("添加任务到用户失败: {}, 任务: {}", userId, taskId, e);
            throw new RuntimeException("添加任务到用户失败", e);
        }
    }

    @Log(module = "同义词管理", operation = "导入", description = "导入同义词")
    @RateLimit(limit = 10, time = 60)
    public void importSynonyms(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".txt")) {
            throw new IllegalArgumentException("只支持TXT格式的文件");
        }
        
        List<SynonymGroup> groups = new ArrayList<>();
        int lineNumber = 0;
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                try {
                    String[] parts = line.split("=>");
                    if (parts.length != 2) {
                        log.warn("第{}行格式错误: {}", lineNumber, line);
                        continue;
                    }
                    
                    String mainWord = parts[0].trim();
                    if (mainWord.isEmpty()) {
                        log.warn("第{}行主词为空", lineNumber);
                        continue;
                    }
                    
                    List<String> terms = new ArrayList<>();
                    terms.add(mainWord);
                    
                    String[] synonyms = parts[1].trim().split(",");
                    for (String word : synonyms) {
                        word = word.trim();
                        if (!word.isEmpty() && !terms.contains(word)) {
                            terms.add(word);
                        }
                    }
                    
                    if (terms.size() < 2) {
                        log.warn("第{}行同义词组至少需要2个词", lineNumber);
                        continue;
                    }
                    
                    SynonymGroup group = SynonymGroup.builder()
                        .terms(terms)
                        .build();
                    groups.add(group);
                } catch (Exception e) {
                    log.error("处理第{}行时发生错误: {}", lineNumber, e.getMessage());
                }
            }
        }
        
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("没有找到有效的同义词组");
        }
        
        // 批量保存同义词组
        synonymService.addSynonymGroups(groups);
        log.info("成功导入{}个同义词组", groups.size());
    }

    @Log(module = "同义词管理", operation = "导出", description = "导出同义词")
    @RateLimit(limit = 20, time = 60)
    public byte[] exportSynonyms() throws IOException {
        List<SynonymGroup> synonymGroups = synonymService.getAllSynonymGroups();
        if (synonymGroups.isEmpty()) {
            return new byte[0];
        }
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(writer)) {
            
            // 写入文件头
            bw.write("# 同义词导出文件");
            bw.newLine();
            bw.write("# 格式: 主词 => 同义词1,同义词2,...");
            bw.newLine();
            bw.write("# 导出时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            bw.newLine();
            bw.newLine();
            
            // 写入同义词组
            for (SynonymGroup group : synonymGroups) {
                List<String> terms = group.getTerms();
                if (terms == null || terms.isEmpty()) {
                    continue;
                }
                
                String mainWord = terms.get(0);
                String line = mainWord + " => " + String.join(",", terms.subList(1, terms.size()));
                bw.write(line);
                bw.newLine();
            }
            
            bw.flush();
            return baos.toByteArray();
        }
    }

    public void validateSynonyms(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".txt")) {
            throw new IllegalArgumentException("只支持TXT格式的文件");
        }
        
        int lineNumber = 0;
        int validGroups = 0;
        Set<String> allWords = new HashSet<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                String[] parts = line.split("=>");
                if (parts.length != 2) {
                    throw new IllegalArgumentException(String.format("第%d行格式错误: %s", lineNumber, line));
                }
                
                String mainWord = parts[0].trim();
                if (mainWord.isEmpty()) {
                    throw new IllegalArgumentException(String.format("第%d行主词为空", lineNumber));
                }
                
                if (mainWord.length() > 100) {
                    throw new IllegalArgumentException(String.format("第%d行主词长度超过100个字符", lineNumber));
                }
                
                String[] synonyms = parts[1].trim().split(",");
                if (synonyms.length < 1) {
                    throw new IllegalArgumentException(String.format("第%d行同义词组至少需要2个词", lineNumber));
                }
                
                Set<String> groupWords = new HashSet<>();
                groupWords.add(mainWord);
                
                for (String word : synonyms) {
                    word = word.trim();
                    if (word.isEmpty()) {
                        throw new IllegalArgumentException(String.format("第%d行存在空词", lineNumber));
                    }
                    
                    if (word.length() > 100) {
                        throw new IllegalArgumentException(String.format("第%d行词长度超过100个字符: %s", lineNumber, word));
                    }
                    
                    if (!groupWords.add(word)) {
                        throw new IllegalArgumentException(String.format("第%d行存在重复词: %s", lineNumber, word));
                    }
                }
                
                // 检查与其他组的词是否重复
                for (String word : groupWords) {
                    if (!allWords.add(word)) {
                        throw new IllegalArgumentException(String.format("第%d行词与其他组重复: %s", lineNumber, word));
                    }
                }
                
                validGroups++;
            }
        }
        
        if (validGroups == 0) {
            throw new IllegalArgumentException("没有找到有效的同义词组");
        }
        
        log.info("验证通过，共{}个同义词组", validGroups);
    }
} 