package com.marsreg.inference.service;

import com.marsreg.inference.model.ModelInfo;
import java.util.List;
import java.util.Map;

public interface ModelManagementService {
    /**
     * 注册新模型
     *
     * @param modelInfo 模型信息
     * @return 注册后的模型信息
     */
    ModelInfo registerModel(ModelInfo modelInfo);
    
    /**
     * 更新模型信息
     *
     * @param modelId 模型ID
     * @param modelInfo 更新的模型信息
     * @return 更新后的模型信息
     */
    ModelInfo updateModel(String modelId, ModelInfo modelInfo);
    
    /**
     * 获取模型信息
     *
     * @param modelId 模型ID
     * @return 模型信息
     */
    ModelInfo getModel(String modelId);
    
    /**
     * 获取所有模型信息
     *
     * @return 模型信息列表
     */
    List<ModelInfo> getAllModels();
    
    /**
     * 删除模型
     *
     * @param modelId 模型ID
     */
    void deleteModel(String modelId);
    
    /**
     * 激活模型
     *
     * @param modelId 模型ID
     * @return 激活后的模型信息
     */
    ModelInfo activateModel(String modelId);
    
    /**
     * 停用模型
     *
     * @param modelId 模型ID
     * @return 停用后的模型信息
     */
    ModelInfo deactivateModel(String modelId);
    
    /**
     * 更新模型指标
     *
     * @param modelId 模型ID
     * @param metrics 模型指标
     * @return 更新后的模型信息
     */
    ModelInfo updateModelMetrics(String modelId, Map<String, Object> metrics);
    
    /**
     * 获取当前活跃模型
     *
     * @return 当前活跃的模型信息
     */
    ModelInfo getActiveModel();
} 