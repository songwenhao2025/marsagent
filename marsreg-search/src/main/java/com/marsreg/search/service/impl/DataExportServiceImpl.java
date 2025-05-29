package com.marsreg.search.service.impl;

import com.marsreg.search.model.SearchStatistics;
import com.marsreg.search.model.UserBehaviorStats;
import com.marsreg.search.service.DataExportService;
import com.marsreg.search.service.SearchStatisticsService;
import com.marsreg.search.service.UserBehaviorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataExportServiceImpl implements DataExportService {

    private final SearchStatisticsService searchStatisticsService;
    private final UserBehaviorService userBehaviorService;
    
    private static final String EXPORT_DIR = "exports";
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public String exportSearchStatistics(LocalDateTime startTime, LocalDateTime endTime, ExportFormat format) {
        try {
            String fileName = createFileName("search_statistics", format);
            Path filePath = createExportFile(fileName);
            
            if (format == ExportFormat.CSV) {
                exportSearchStatisticsToCsv(filePath, startTime, endTime);
            } else {
                exportSearchStatisticsToExcel(filePath, startTime, endTime);
            }
            
            return filePath.toString();
        } catch (Exception e) {
            log.error("Failed to export search statistics", e);
            throw new RuntimeException("导出搜索统计数据失败", e);
        }
    }

    @Override
    public String exportUserBehaviorStats(List<String> userIds, LocalDateTime startTime, LocalDateTime endTime, ExportFormat format) {
        try {
            String fileName = createFileName("user_behavior", format);
            Path filePath = createExportFile(fileName);
            
            if (format == ExportFormat.CSV) {
                exportUserBehaviorStatsToCsv(filePath, userIds, startTime, endTime);
            } else {
                exportUserBehaviorStatsToExcel(filePath, userIds, startTime, endTime);
            }
            
            return filePath.toString();
        } catch (Exception e) {
            log.error("Failed to export user behavior stats", e);
            throw new RuntimeException("导出用户行为统计数据失败", e);
        }
    }

    @Override
    public String exportPerformanceMetrics(LocalDateTime startTime, LocalDateTime endTime, ExportFormat format) {
        try {
            String fileName = createFileName("performance_metrics", format);
            Path filePath = createExportFile(fileName);
            
            if (format == ExportFormat.CSV) {
                exportPerformanceMetricsToCsv(filePath, startTime, endTime);
            } else {
                exportPerformanceMetricsToExcel(filePath, startTime, endTime);
            }
            
            return filePath.toString();
        } catch (Exception e) {
            log.error("Failed to export performance metrics", e);
            throw new RuntimeException("导出性能指标数据失败", e);
        }
    }

    @Override
    public String exportHotKeywords(int size, ExportFormat format) {
        try {
            String fileName = createFileName("hot_keywords", format);
            Path filePath = createExportFile(fileName);
            
            if (format == ExportFormat.CSV) {
                exportHotKeywordsToCsv(filePath, size);
            } else {
                exportHotKeywordsToExcel(filePath, size);
            }
            
            return filePath.toString();
        } catch (Exception e) {
            log.error("Failed to export hot keywords", e);
            throw new RuntimeException("导出热门关键词数据失败", e);
        }
    }

    private String createFileName(String prefix, ExportFormat format) {
        String timestamp = LocalDateTime.now().format(FILE_NAME_FORMATTER);
        String extension = format == ExportFormat.CSV ? ".csv" : ".xlsx";
        return prefix + "_" + timestamp + extension;
    }

    private Path createExportFile(String fileName) throws IOException {
        Path exportDir = Paths.get(EXPORT_DIR);
        if (!Files.exists(exportDir)) {
            Files.createDirectories(exportDir);
        }
        return exportDir.resolve(fileName);
    }

    private void exportSearchStatisticsToCsv(Path filePath, LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(filePath.toFile()), CSVFormat.DEFAULT)) {
            // 写入表头
            printer.printRecord("统计指标", "数值");
            
            // 获取统计数据
            var stats = searchStatisticsService.getStatisticsByTimeRange(startTime, endTime);
            
            // 写入数据
            printer.printRecord("总搜索次数", stats.getTotalSearches());
            printer.printRecord("独立用户数", stats.getUniqueUsers());
            printer.printRecord("平均响应时间(ms)", stats.getAverageResponseTime());
            
            // 写入搜索类型分布
            printer.printRecord("搜索类型分布", "");
            for (Map.Entry<String, Long> entry : stats.getSearchTypeDistribution().entrySet()) {
                try {
                    printer.printRecord(entry.getKey(), entry.getValue());
                } catch (IOException e) {
                    log.error("Failed to write search type distribution", e);
                }
            }
            
            // 写入文档类型分布
            printer.printRecord("文档类型分布", "");
            for (Map.Entry<String, Long> entry : stats.getDocumentTypeDistribution().entrySet()) {
                try {
                    printer.printRecord(entry.getKey(), entry.getValue());
                } catch (IOException e) {
                    log.error("Failed to write document type distribution", e);
                }
            }
        }
    }

    private void exportSearchStatisticsToExcel(Path filePath, LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("搜索统计");
            
            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // 写入表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("统计指标");
            headerRow.createCell(1).setCellValue("数值");
            headerRow.getCell(0).setCellStyle(headerStyle);
            headerRow.getCell(1).setCellStyle(headerStyle);
            
            // 获取统计数据
            var stats = searchStatisticsService.getStatisticsByTimeRange(startTime, endTime);
            
            // 写入数据
            int rowNum = 1;
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("总搜索次数");
            row.createCell(1).setCellValue(stats.getTotalSearches());
            
            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("独立用户数");
            row.createCell(1).setCellValue(stats.getUniqueUsers());
            
            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("平均响应时间(ms)");
            row.createCell(1).setCellValue(stats.getAverageResponseTime());
            
            // 写入搜索类型分布
            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("搜索类型分布");
            rowNum++;
            
            for (Map.Entry<String, Long> entry : stats.getSearchTypeDistribution().entrySet()) {
                row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue());
            }
            
            // 写入文档类型分布
            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("文档类型分布");
            rowNum++;
            
            for (Map.Entry<String, Long> entry : stats.getDocumentTypeDistribution().entrySet()) {
                row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue());
            }
            
            // 自动调整列宽
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            
            // 写入文件
            try (var outputStream = Files.newOutputStream(filePath)) {
                workbook.write(outputStream);
            }
        }
    }

    private void exportUserBehaviorStatsToCsv(Path filePath, List<String> userIds, LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(filePath.toFile()), CSVFormat.DEFAULT)) {
            // 写入表头
            printer.printRecord("用户ID", "搜索次数", "平均响应时间(ms)", "常用关键词", "活跃时间段");
            
            // 获取用户行为统计数据
            for (String userId : userIds) {
                var stats = userBehaviorService.getUserBehaviorStats(userId);
                printer.printRecord(
                    userId,
                    stats.getSearchCount(),
                    stats.getAverageResponseTime(),
                    String.join(",", stats.getFrequentKeywords()),
                    stats.getActiveTimeDistribution().toString()
                );
            }
        }
    }

    private void exportUserBehaviorStatsToExcel(Path filePath, List<String> userIds, LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("用户行为统计");
            
            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // 写入表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("用户ID");
            headerRow.createCell(1).setCellValue("搜索次数");
            headerRow.createCell(2).setCellValue("平均响应时间(ms)");
            headerRow.createCell(3).setCellValue("常用关键词");
            headerRow.createCell(4).setCellValue("活跃时间段");
            
            for (int i = 0; i < 5; i++) {
                headerRow.getCell(i).setCellStyle(headerStyle);
            }
            
            // 获取用户行为统计数据
            int rowNum = 1;
            for (String userId : userIds) {
                var stats = userBehaviorService.getUserBehaviorStats(userId);
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(userId);
                row.createCell(1).setCellValue(stats.getSearchCount());
                row.createCell(2).setCellValue(stats.getAverageResponseTime());
                row.createCell(3).setCellValue(String.join(",", stats.getFrequentKeywords()));
                row.createCell(4).setCellValue(stats.getActiveTimeDistribution().toString());
            }
            
            // 自动调整列宽
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 写入文件
            try (var outputStream = Files.newOutputStream(filePath)) {
                workbook.write(outputStream);
            }
        }
    }

    private void exportPerformanceMetricsToCsv(Path filePath, LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(filePath.toFile()), CSVFormat.DEFAULT)) {
            // 写入表头
            printer.printRecord("指标", "数值");
            
            // 获取性能指标
            var metrics = searchStatisticsService.getPerformanceMetrics();
            
            // 写入数据
            printer.printRecord("平均响应时间(ms)", metrics.get("averageResponseTime"));
            printer.printRecord("最大响应时间(ms)", metrics.get("maxResponseTime"));
            printer.printRecord("最小响应时间(ms)", metrics.get("minResponseTime"));
            printer.printRecord("95分位响应时间(ms)", metrics.get("p95ResponseTime"));
            printer.printRecord("99分位响应时间(ms)", metrics.get("p99ResponseTime"));
        }
    }

    private void exportPerformanceMetricsToExcel(Path filePath, LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("性能指标");
            
            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // 写入表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("指标");
            headerRow.createCell(1).setCellValue("数值");
            headerRow.getCell(0).setCellStyle(headerStyle);
            headerRow.getCell(1).setCellStyle(headerStyle);
            
            // 获取性能指标
            var metrics = searchStatisticsService.getPerformanceMetrics();
            
            // 写入数据
            int rowNum = 1;
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("平均响应时间(ms)");
            row.createCell(1).setCellValue((Double) metrics.get("averageResponseTime"));
            
            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("最大响应时间(ms)");
            row.createCell(1).setCellValue((Double) metrics.get("maxResponseTime"));
            
            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("最小响应时间(ms)");
            row.createCell(1).setCellValue((Double) metrics.get("minResponseTime"));
            
            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("95分位响应时间(ms)");
            row.createCell(1).setCellValue((Double) metrics.get("p95ResponseTime"));
            
            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("99分位响应时间(ms)");
            row.createCell(1).setCellValue((Double) metrics.get("p99ResponseTime"));
            
            // 自动调整列宽
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            
            // 写入文件
            try (var outputStream = Files.newOutputStream(filePath)) {
                workbook.write(outputStream);
            }
        }
    }

    private void exportHotKeywordsToCsv(Path filePath, int size) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(filePath.toFile()), CSVFormat.DEFAULT)) {
            // 写入表头
            printer.printRecord("关键词", "搜索次数");
            
            // 获取热门关键词
            List<SearchStatistics.KeywordStats> keywordList = searchStatisticsService.getHotKeywords(size);
            for (SearchStatistics.KeywordStats keyword : keywordList) {
                printer.printRecord(keyword.getKeyword(), keyword.getCount());
            }
        }
    }

    private void exportHotKeywordsToExcel(Path filePath, int size) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("热门关键词");
            
            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // 写入表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("关键词");
            headerRow.createCell(1).setCellValue("搜索次数");
            headerRow.getCell(0).setCellStyle(headerStyle);
            headerRow.getCell(1).setCellStyle(headerStyle);
            
            // 获取热门关键词
            int rowNum = 1;
            List<SearchStatistics.KeywordStats> keywordList = searchStatisticsService.getHotKeywords(size);
            for (SearchStatistics.KeywordStats keyword : keywordList) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(keyword.getKeyword());
                row.createCell(1).setCellValue(keyword.getCount());
            }
            
            // 自动调整列宽
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            
            // 写入文件
            try (var outputStream = Files.newOutputStream(filePath)) {
                workbook.write(outputStream);
            }
        }
    }
} 