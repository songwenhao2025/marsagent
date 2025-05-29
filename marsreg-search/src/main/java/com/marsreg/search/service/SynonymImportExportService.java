package com.marsreg.search.service;

import com.marsreg.search.model.SynonymGroup;
import com.marsreg.search.model.SynonymImportExportTask;
import java.util.List;

public interface SynonymImportExportService {
    /**
     * 导出同义词组到文件
     * @param format 导出格式（CSV/JSON/XML/EXCEL）
     * @param category 分类（可选）
     * @return 文件路径
     */
    String exportSynonyms(String format, String category);
    
    /**
     * 从文件导入同义词组
     * @param filePath 文件路径
     * @param format 导入格式（CSV/JSON/XML/EXCEL）
     * @return 导入的同义词组数量
     */
    int importSynonyms(String filePath, String format);
    
    /**
     * 导出同义词组到字符串
     * @param groups 同义词组列表
     * @param format 导出格式（CSV/JSON/XML）
     * @return 导出的字符串
     */
    String exportSynonymsToString(List<SynonymGroup> groups, String format);
    
    /**
     * 从字符串导入同义词组
     * @param content 字符串内容
     * @param format 导入格式（CSV/JSON/XML）
     * @return 导入的同义词组列表
     */
    List<SynonymGroup> importSynonymsFromString(String content, String format);
    
    /**
     * 验证同义词组数据
     * @param groups 同义词组列表
     * @return 验证结果
     */
    boolean validateSynonyms(List<SynonymGroup> groups);
    
    /**
     * 获取支持的导入/导出格式
     * @return 格式列表
     */
    List<String> getSupportedFormats();
    
    /**
     * 获取格式的MIME类型
     * @param format 格式
     * @return MIME类型
     */
    String getMediaType(String format);
    
    /**
     * 创建批量导入任务
     * @param userId 用户ID
     * @param filePath 文件路径
     * @param format 导入格式
     * @return 任务ID
     */
    String createImportTask(String userId, String filePath, String format);
    
    /**
     * 创建批量导出任务
     * @param userId 用户ID
     * @param format 导出格式
     * @param category 分类（可选）
     * @return 任务ID
     */
    String createExportTask(String userId, String format, String category);
    
    /**
     * 获取任务状态
     * @param taskId 任务ID
     * @return 任务状态
     */
    SynonymImportExportTask getTaskStatus(String taskId);
    
    /**
     * 取消任务
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    boolean cancelTask(String taskId);
    
    /**
     * 获取用户的任务列表
     * @param userId 用户ID
     * @return 任务列表
     */
    List<SynonymImportExportTask> getUserTasks(String userId);
    
    /**
     * 清理已完成的任务
     * @param days 保留天数
     * @return 清理的任务数量
     */
    int cleanupTasks(int days);
} 