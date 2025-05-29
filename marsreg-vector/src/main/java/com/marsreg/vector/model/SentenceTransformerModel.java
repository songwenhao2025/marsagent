package com.marsreg.vector.model;

import ai.djl.Device;
import ai.djl.inference.Predictor;
import ai.djl.modality.Input;
import ai.djl.modality.Output;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import com.marsreg.vector.config.VectorizationConfig;
import com.marsreg.vector.exception.VectorizationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SentenceTransformerModel implements VectorizationModel {
    
    private final VectorizationConfig config;
    private final AtomicReference<ZooModel<Input, Output>> modelRef = new AtomicReference<>();
    private final ReentrantReadWriteLock modelLock = new ReentrantReadWriteLock();
    private volatile boolean isWarmedUp = false;
    
    // 预测器缓存
    private final ThreadLocal<Predictor<Input, Output>> predictorCache = new ThreadLocal<>();
    private static final int PREDICTOR_CACHE_TIMEOUT = 300; // 5分钟
    
    @Override
    public String getModelName() {
        return config.getModel().getModelName();
    }
    
    @Override
    public String getModelVersion() {
        return config.getModel().getModelVersion();
    }
    
    @Override
    public int getDimension() {
        return config.getModel().getVectorDimension();
    }
    
    @Override
    public List<Float> vectorize(String text) {
        try {
            Predictor<Input, Output> predictor = getPredictor();
            try {
                Input input = new Input();
                input.add(text);
                Output output = predictor.predict(input);
                return parseOutput(output);
            } finally {
                // 不关闭预测器，而是返回缓存
                returnPredictor(predictor);
            }
        } catch (Exception e) {
            log.error("文本向量化失败", e);
            throw new VectorizationException("文本向量化失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<List<Float>> batchVectorize(List<String> texts) {
        if (texts.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            Predictor<Input, Output> predictor = getPredictor();
            try {
                Input input = new Input();
                for (String text : texts) {
                    input.add(text);
                }
                Output output = predictor.predict(input);
                return parseBatchOutput(output, texts.size());
            } finally {
                // 不关闭预测器，而是返回缓存
                returnPredictor(predictor);
            }
        } catch (Exception e) {
            log.error("批量文本向量化失败", e);
            throw new VectorizationException("批量文本向量化失败: " + e.getMessage());
        }
    }
    
    @Override
    public void warmup() {
        if (isWarmedUp) {
            return;
        }
        
        try {
            modelLock.writeLock().lock();
            try {
                if (isWarmedUp) {
                    return;
                }
                
                // 使用一些常见文本进行预热
                List<String> warmupTexts = List.of(
                    "这是一个测试文本",
                    "Hello, world!",
                    "测试向量化模型",
                    "人工智能和机器学习",
                    "自然语言处理技术"
                );
                
                // 预热模型
                batchVectorize(warmupTexts);
                isWarmedUp = true;
                log.info("向量化模型预热完成");
            } finally {
                modelLock.writeLock().unlock();
            }
        } catch (Exception e) {
            log.error("向量化模型预热失败", e);
            throw new VectorizationException("向量化模型预热失败: " + e.getMessage());
        }
    }
    
    @Override
    public void update(String modelPath) {
        try {
            modelLock.writeLock().lock();
            try {
                // 关闭所有预测器
                closeAllPredictors();
                
                // 关闭旧模型
                ZooModel<Input, Output> oldModel = modelRef.get();
                if (oldModel != null) {
                    oldModel.close();
                }
                
                // 加载新模型
                Criteria<Input, Output> criteria = Criteria.builder()
                    .setTypes(Input.class, Output.class)
                    .optModelPath(java.nio.file.Paths.get(modelPath))
                    .optDevice(Device.cpu())
                    .optProgress(new ProgressBar())
                    .build();
                
                ZooModel<Input, Output> newModel = criteria.loadModel();
                modelRef.set(newModel);
                isWarmedUp = false;
                
                // 预热新模型
                warmup();
                
                log.info("向量化模型更新完成");
            } finally {
                modelLock.writeLock().unlock();
            }
        } catch (Exception e) {
            log.error("更新向量化模型失败", e);
            throw new VectorizationException("更新向量化模型失败: " + e.getMessage());
        }
    }
    
    private ZooModel<Input, Output> getModel() {
        ZooModel<Input, Output> model = modelRef.get();
        if (model == null) {
            modelLock.writeLock().lock();
            try {
                model = modelRef.get();
                if (model == null) {
                    Criteria<Input, Output> criteria = Criteria.builder()
                        .setTypes(Input.class, Output.class)
                        .optModelPath(java.nio.file.Paths.get(config.getModel().getModelPath()))
                        .optDevice(Device.cpu())
                        .optProgress(new ProgressBar())
                        .build();
                    
                    model = criteria.loadModel();
                    modelRef.set(model);
                }
            } finally {
                modelLock.writeLock().unlock();
            }
        }
        return model;
    }
    
    private Predictor<Input, Output> getPredictor() {
        Predictor<Input, Output> predictor = predictorCache.get();
        if (predictor == null) {
            predictor = getModel().newPredictor();
            predictorCache.set(predictor);
        }
        return predictor;
    }
    
    private void returnPredictor(Predictor<Input, Output> predictor) {
        // 预测器已经在缓存中，不需要额外操作
    }
    
    private void closeAllPredictors() {
        Predictor<Input, Output> predictor = predictorCache.get();
        if (predictor != null) {
            predictor.close();
            predictorCache.remove();
        }
    }
    
    private List<Float> parseOutput(Output output) {
        float[] data = output.getData().getAsFloatArray();
        List<Float> vector = new ArrayList<>(data.length);
        for (float v : data) {
            vector.add(v);
        }
        return vector;
    }
    
    private List<List<Float>> parseBatchOutput(Output output, int batchSize) {
        float[] data = output.getData().getAsFloatArray();
        int dimension = getDimension();
        List<List<Float>> vectors = new ArrayList<>(batchSize);
        
        for (int i = 0; i < batchSize; i++) {
            List<Float> vector = new ArrayList<>(dimension);
            for (int j = 0; j < dimension; j++) {
                vector.add(data[i * dimension + j]);
            }
            vectors.add(vector);
        }
        
        return vectors;
    }
    
    // 在Spring容器关闭时调用
    public void destroy() {
        closeAllPredictors();
        ZooModel<Input, Output> model = modelRef.get();
        if (model != null) {
            model.close();
        }
    }
} 