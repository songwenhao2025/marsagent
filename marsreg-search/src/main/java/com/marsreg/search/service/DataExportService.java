package com.marsreg.search.service;

import java.time.LocalDateTime;
import java.util.List;

public interface DataExportService {
    /**
     * 导出搜索统计数据
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param format 导出格式
     * @return 导出文件路径
     */
    String exportSearchStatistics(LocalDateTime startTime, LocalDateTime endTime, ExportFormat format);
    
    /**
     * 导出用户行为统计数据
     *
     * @param userIds 用户ID列表
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param format 导出格式
     * @return 导出文件路径
     */
    String exportUserBehaviorStats(List<String> userIds, LocalDateTime startTime, LocalDateTime endTime, ExportFormat format);
    
    /**
     * 导出性能指标数据
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param format 导出格式
     * @return 导出文件路径
     */
    String exportPerformanceMetrics(LocalDateTime startTime, LocalDateTime endTime, ExportFormat format);
    
    /**
     * 导出热门关键词数据
     *
     * @param size 导出数量
     * @param format 导出格式
     * @return 导出文件路径
     */
    String exportHotKeywords(int size, ExportFormat format);
    
    /**
     * 导出格式枚举
     */
    enum ExportFormat {
        CSV,
        EXCEL
    }
} 