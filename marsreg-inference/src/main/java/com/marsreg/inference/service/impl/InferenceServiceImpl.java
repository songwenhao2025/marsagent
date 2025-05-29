package com.marsreg.inference.service.impl;

import com.marsreg.inference.config.InferenceProperties;
import com.marsreg.inference.exception.ErrorCode;
import com.marsreg.inference.exception.InferenceException;
import com.marsreg.inference.model.InferenceRequest;
import com.marsreg.inference.model.InferenceResponse;
import com.marsreg.inference.model.ModelInfo;
import com.marsreg.inference.service.InferenceService;
import com.marsreg.inference.service.LLMService;
import com.marsreg.inference.service.MetricsService;
import com.marsreg.inference.service.ModelManagementService;
import com.marsreg.search.model.SearchRequest;
import com.marsreg.search.model.SearchResult;
import com.marsreg.search.service.SearchService;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.marsreg.common.util.StringUtils;
import com.marsreg.common.annotation.Cache;
import com.marsreg.common.annotation.Log;
import com.marsreg.common.annotation.RateLimit;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class InferenceServiceImpl implements InferenceService {

    private final SearchService searchService;
    private final LLMService llmService;
    private final InferenceProperties properties;
    private final MetricsService metricsService;
    private final ModelManagementService modelManagementService;

    @PostConstruct
    public void init() {
        metricsService.init();
        initDefaultModel();
    }

    private void initDefaultModel() {
        try {
            ModelInfo activeModel = modelManagementService.getActiveModel();
            if (activeModel == null) {
                // 创建默认模型
                ModelInfo defaultModel = ModelInfo.builder()
                    .name(properties.getModel().getName())
                    .version(properties.getModel().getVersion())
                    .description("默认推理模型")
                    .provider("openai")
                    .parameters(properties.getModel().getParameters())
                    .status(ModelInfo.Status.ACTIVE.name())
                    .build();
                
                modelManagementService.registerModel(defaultModel);
                modelManagementService.activateModel(defaultModel.getId());
                log.info("默认模型初始化成功");
            }
        } catch (Exception e) {
            log.error("默认模型初始化失败", e);
        }
    }

    @Override
    @Log(module = "推理服务", operation = "推理", description = "执行推理请求")
    @Cache(name = "inferenceResults", 
           key = "#request.hashCode()", 
           condition = "#root.target.properties.cache.enabled",
           expire = 3600,
           timeUnit = TimeUnit.SECONDS)
    @RateLimit(key = "#request.question", limit = 100, time = 60, timeUnit = TimeUnit.SECONDS)
    public InferenceResponse infer(InferenceRequest request) {
        Timer.Sample sample = metricsService.startInferenceTimer();
        try {
            validateRequest(request);
            
            // 获取当前活跃模型
            ModelInfo activeModel = modelManagementService.getActiveModel();
            if (activeModel == null) {
                throw new InferenceException(
                    ErrorCode.MODEL_NOT_FOUND.getCode(),
                    "未找到活跃模型"
                );
            }
            
            // 1. 执行文档检索
            List<SearchResult> searchResults = searchService.search(buildSearchRequest(request));
            if (searchResults.isEmpty()) {
                metricsService.recordError();
                throw new InferenceException(
                    ErrorCode.CONTEXT_EXTRACTION_FAILED.getCode(),
                    "未找到相关文档"
                );
            }
            
            // 2. 提取上下文
            List<String> context = extractContext(searchResults);
            if (context.isEmpty()) {
                metricsService.recordError();
                throw new InferenceException(
                    ErrorCode.CONTEXT_EXTRACTION_FAILED.getCode(),
                    "上下文提取失败"
                );
            }
            
            // 更新上下文大小指标
            metricsService.updateContextSize(context.size());
            
            // 3. 生成回答
            Map<String, Object> modelParams = new HashMap<>(activeModel.getParameters());
            if (request.getParameters() != null) {
                modelParams.putAll(request.getParameters());
            }
            
            String answer = llmService.generateAnswer(request.getQuestion(), context, modelParams);
            if (StringUtils.isEmpty(answer)) {
                metricsService.recordError();
                throw new InferenceException(
                    ErrorCode.MODEL_INFERENCE_FAILED.getCode(),
                    "模型生成回答失败"
                );
            }
            
            // 4. 更新模型指标
            updateModelMetrics(activeModel, searchResults, answer);
            
            // 5. 构建响应
            return InferenceResponse.builder()
                .answer(answer)
                .references(buildReferences(searchResults))
                .metadata(buildMetadata(request, searchResults, activeModel))
                .build();
                
        } catch (Exception e) {
            log.error("推理过程发生错误", e);
            metricsService.recordError();
            if (e instanceof InferenceException) {
                throw (InferenceException) e;
            }
            throw new InferenceException(
                ErrorCode.INTERNAL_ERROR.getCode(),
                "推理过程发生错误: " + e.getMessage(),
                e
            );
        } finally {
            metricsService.stopTimer(sample, metricsService.getInferenceTimer());
        }
    }

    @Override
    @Log(module = "推理服务", operation = "流式推理", description = "执行流式推理请求")
    @RateLimit(key = "#request.question", limit = 50, time = 60, timeUnit = TimeUnit.SECONDS)
    public void inferStream(InferenceRequest request, StreamCallback callback) {
        Timer.Sample sample = metricsService.startStreamInferenceTimer();
        try {
            validateRequest(request);
            
            // 获取当前活跃模型
            ModelInfo activeModel = modelManagementService.getActiveModel();
            if (activeModel == null) {
                throw new InferenceException(
                    ErrorCode.MODEL_NOT_FOUND.getCode(),
                    "未找到活跃模型"
                );
            }
            
            // 1. 执行文档检索
            List<SearchResult> searchResults = searchService.search(buildSearchRequest(request));
            if (searchResults.isEmpty()) {
                metricsService.recordError();
                callback.onError(new InferenceException(
                    ErrorCode.CONTEXT_EXTRACTION_FAILED.getCode(),
                    "未找到相关文档"
                ));
                return;
            }
            
            // 2. 提取上下文
            List<String> context = extractContext(searchResults);
            if (context.isEmpty()) {
                metricsService.recordError();
                callback.onError(new InferenceException(
                    ErrorCode.CONTEXT_EXTRACTION_FAILED.getCode(),
                    "上下文提取失败"
                ));
                return;
            }
            
            // 更新上下文大小指标
            metricsService.updateContextSize(context.size());
            
            // 3. 流式生成回答
            Map<String, Object> modelParams = new HashMap<>(activeModel.getParameters());
            if (request.getParameters() != null) {
                modelParams.putAll(request.getParameters());
            }
            
            llmService.generateAnswerStream(request.getQuestion(), context, modelParams,
                new LLMService.StreamCallback() {
                    @Override
                    public void onToken(String token) {
                        callback.onToken(token);
                    }
                    
                    @Override
                    public void onComplete() {
                        // 更新模型指标
                        updateModelMetrics(activeModel, searchResults, null);
                        callback.onComplete();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        metricsService.recordError();
                        callback.onError(new InferenceException(
                            ErrorCode.STREAM_PROCESSING_FAILED.getCode(),
                            "流式推理处理失败: " + error.getMessage(),
                            error
                        ));
                    }
                });
                
        } catch (Exception e) {
            log.error("流式推理过程发生错误", e);
            metricsService.recordError();
            callback.onError(new InferenceException(
                ErrorCode.STREAM_INITIALIZATION_FAILED.getCode(),
                "流式推理初始化失败: " + e.getMessage(),
                e
            ));
        } finally {
            metricsService.stopTimer(sample, metricsService.getStreamInferenceTimer());
        }
    }

    private void updateModelMetrics(ModelInfo model, List<SearchResult> searchResults, String answer) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("lastUsedTime", LocalDateTime.now());
        metrics.put("documentCount", searchResults.size());
        metrics.put("averageRelevance", searchResults.stream()
            .mapToDouble(SearchResult::getScore)
            .average()
            .orElse(0.0));
        if (answer != null) {
            metrics.put("answerLength", answer.length());
        }
        
        modelManagementService.updateModelMetrics(model.getId(), metrics);
    }

    private Map<String, Object> buildMetadata(InferenceRequest request, List<SearchResult> searchResults, ModelInfo model) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("searchType", request.getSearchType());
        metadata.put("documentCount", searchResults.size());
        metadata.put("averageRelevance", searchResults.stream()
            .mapToDouble(SearchResult::getScore)
            .average()
            .orElse(0.0));
        metadata.put("modelId", model.getId());
        metadata.put("modelName", model.getName());
        metadata.put("modelVersion", model.getVersion());
        return metadata;
    }

    private void validateRequest(InferenceRequest request) {
        if (request == null) {
            throw new InferenceException(
                ErrorCode.INVALID_REQUEST.getCode(),
                "请求不能为空"
            );
        }
        
        if (StringUtils.isEmpty(request.getQuestion())) {
            throw new InferenceException(
                ErrorCode.INVALID_REQUEST.getCode(),
                "问题不能为空"
            );
        }
        
        if (request.getSearchType() == null) {
            throw new InferenceException(
                ErrorCode.INVALID_REQUEST.getCode(),
                "搜索类型不能为空"
            );
        }
        
        if (request.getMaxDocuments() != null && 
            request.getMaxDocuments() > properties.getContext().getMaxDocuments()) {
            throw new InferenceException(
                ErrorCode.CONTEXT_TOO_LARGE.getCode(),
                "请求的文档数量超过最大限制: " + properties.getContext().getMaxDocuments()
            );
        }
    }

    private SearchRequest buildSearchRequest(InferenceRequest request) {
        return SearchRequest.builder()
            .query(request.getQuestion())
            .searchType(request.getSearchType())
            .size(request.getMaxDocuments() != null ? 
                request.getMaxDocuments() : 
                properties.getContext().getMaxDocuments())
            .minSimilarity(request.getMinSimilarity() != null ? 
                request.getMinSimilarity() : 
                properties.getContext().getMinSimilarity())
            .documentTypes(request.getDocumentTypes())
            .build();
    }

    private List<String> extractContext(List<SearchResult> searchResults) {
        return searchResults.stream()
            .map(result -> String.format("标题：%s\n内容：%s", 
                result.getTitle(), result.getContent()))
            .collect(Collectors.toList());
    }

    private List<InferenceResponse.DocumentReference> buildReferences(List<SearchResult> searchResults) {
        return searchResults.stream()
            .map(result -> InferenceResponse.DocumentReference.builder()
                .documentId(result.getDocumentId())
                .title(result.getTitle())
                .content(result.getContent())
                .relevance(result.getScore())
                .metadata(result.getMetadata())
                .build())
            .collect(Collectors.toList());
    }
} 