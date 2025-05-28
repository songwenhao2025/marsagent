package com.marsreg.vector.service.impl;

import ai.djl.Device;
import ai.djl.inference.Predictor;
import ai.djl.modality.Input;
import ai.djl.modality.Output;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import com.marsreg.vector.service.VectorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@Profile("!test")
public class HuggingFaceVectorizationService implements VectorizationService {

    @Value("${marsreg.vector.huggingface.model:sentence-transformers/all-MiniLM-L6-v2}")
    private String modelName;

    @Value("${marsreg.vector.huggingface.dimension:384}")
    private int dimension;

    @Value("${marsreg.vector.huggingface.thread-pool-size:4}")
    private int threadPoolSize;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    private ZooModel<Input, Output> model;
    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        // 测试环境下不加载模型，直接跳过
        if (activeProfile != null && activeProfile.contains("test")) {
            return;
        }
        try {
            // 创建模型加载条件
            Criteria<Input, Output> criteria = Criteria.builder()
                    .setTypes(Input.class, Output.class)
                    .optModelPath(Paths.get("/models"))
                    .optModelName(modelName)
                    .optEngine("PyTorch")
                    .optProgress(new ProgressBar())
                    .optDevice(Device.cpu())
                    .build();

            // 加载模型
            model = criteria.loadModel();
            executorService = Executors.newFixedThreadPool(threadPoolSize);
            log.info("HuggingFace模型加载成功: {}", modelName);
        } catch (Exception e) {
            log.error("HuggingFace模型加载失败", e);
            throw new RuntimeException("HuggingFace模型加载失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (model != null) {
            model.close();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public float[] vectorize(String text) {
        try (Predictor<Input, Output> predictor = model.newPredictor()) {
            Input input = new Input();
            input.add(text);
            Output output = predictor.predict(input);
            try (NDManager manager = NDManager.newBaseManager()) {
                NDList ndList = output.getAsNDList(manager, 0);
                NDArray ndArray = ndList.get(0);
                return ndArray.toFloatArray();
            }
        } catch (Exception e) {
            log.error("文本向量化失败", e);
            throw new RuntimeException("文本向量化失败", e);
        }
    }

    @Override
    public List<float[]> batchVectorize(List<String> texts) {
        List<CompletableFuture<float[]>> futures = new ArrayList<>();
        
        for (String text : texts) {
            CompletableFuture<float[]> future = CompletableFuture.supplyAsync(() -> {
                try (Predictor<Input, Output> predictor = model.newPredictor()) {
                    Input input = new Input();
                    input.add(text);
                    Output output = predictor.predict(input);
                    try (NDManager manager = NDManager.newBaseManager()) {
                        NDList ndList = output.getAsNDList(manager, 0);
                        NDArray ndArray = ndList.get(0);
                        return ndArray.toFloatArray();
                    }
                } catch (Exception e) {
                    log.error("批量文本向量化失败", e);
                    throw new RuntimeException("批量文本向量化失败", e);
                }
            }, executorService);
            futures.add(future);
        }

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    @Override
    public float calculateSimilarity(float[] vector1, float[] vector2) {
        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        return dotProduct / (float) Math.sqrt(norm1 * norm2);
    }

    @Override
    public float[] normalize(float[] vector) {
        float norm = 0.0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }
        return normalized;
    }

    @Override
    public int getDimension() {
        return dimension;
    }
} 