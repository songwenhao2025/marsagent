package com.marsreg.vector.model;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.modality.Input;
import ai.djl.modality.Output;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import com.marsreg.vector.config.VectorizationConfig;
import com.marsreg.vector.exception.VectorizationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SentenceTransformerModel implements VectorizationModel {
    
    private final VectorizationConfig.Model config;
    private final AtomicReference<ZooModel<Input, Output>> modelRef = new AtomicReference<>();
    private final ReentrantReadWriteLock modelLock = new ReentrantReadWriteLock();
    private volatile boolean isWarmedUp = false;
    
    // 预测器缓存
    private final ThreadLocal<Predictor<Input, Output>> predictorCache = new ThreadLocal<>();
    private static final int PREDICTOR_CACHE_TIMEOUT = 300; // 5分钟
    
    private final Model model;
    
    @Override
    public String getModelName() {
        return config.getName();
    }
    
    @Override
    public String getModelVersion() {
        return config.getVersion();
    }
    
    @Override
    public int getDimension() {
        return config.getDimension();
    }
    
    public String getModelPath() {
        return config.getPath();
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
                encode(warmupTexts);
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
        try {
            ZooModel<Input, Output> model = modelRef.get();
            if (model == null) {
                modelLock.writeLock().lock();
                try {
                    model = modelRef.get();
                    if (model == null) {
                        Criteria<Input, Output> criteria = Criteria.builder()
                            .setTypes(Input.class, Output.class)
                            .optModelPath(java.nio.file.Paths.get(config.getPath()))
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
        } catch (ai.djl.repository.zoo.ModelNotFoundException | ai.djl.MalformedModelException | java.io.IOException e) {
            log.error("加载模型失败", e);
            throw new RuntimeException("加载模型失败", e);
        }
    }
    
    private Predictor<Input, Output> getPredictor() {
        try {
            Predictor<Input, Output> predictor = predictorCache.get();
            if (predictor == null) {
                predictor = getModel().newPredictor();
                predictorCache.set(predictor);
            }
            return predictor;
        } catch (Exception e) {
            log.error("获取预测器失败", e);
            throw new RuntimeException("获取预测器失败", e);
        }
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
    
    // 在Spring容器关闭时调用
    public void destroy() {
        closeAllPredictors();
        ZooModel<Input, Output> model = modelRef.get();
        if (model != null) {
            model.close();
        }
    }

    public float[] encode(String text) {
        try {
            Translator<String, float[]> translator = new TextEmbeddingTranslator();
            try (Predictor<String, float[]> predictor = model.newPredictor(translator)) {
                return predictor.predict(text);
            }
        } catch (Exception e) {
            log.error("文本编码失败: {}", text, e);
            throw new RuntimeException("文本编码失败", e);
        }
    }

    public List<float[]> encode(List<String> texts) {
        try {
            Translator<String, float[]> translator = new TextEmbeddingTranslator();
            try (Predictor<String, float[]> predictor = model.newPredictor(translator)) {
                return texts.stream()
                    .map(text -> {
                        try {
                            return predictor.predict(text);
                        } catch (Exception e) {
                            log.error("文本编码失败: {}", text, e);
                            throw new RuntimeException("文本编码失败", e);
                        }
                    })
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("批量文本编码失败", e);
            throw new RuntimeException("批量文本编码失败", e);
        }
    }

    @Override
    public List<List<Float>> batchVectorize(List<String> texts) {
        List<float[]> vectors = encode(texts);
        return vectors.stream()
            .map(vector -> {
                List<Float> result = new ArrayList<>(vector.length);
                for (float v : vector) {
                    result.add(v);
                }
                return result;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<Float> vectorize(String text) {
        float[] vector = encode(text);
        List<Float> result = new ArrayList<>(vector.length);
        for (float v : vector) {
            result.add(v);
        }
        return result;
    }

    private static class TextEmbeddingTranslator implements Translator<String, float[]> {
        @Override
        public float[] processOutput(TranslatorContext ctx, NDList list) {
            NDArray array = list.get(0);
            return array.toFloatArray();
        }

        @Override
        public NDList processInput(TranslatorContext ctx, String input) {
            NDList list = new NDList();
            list.add(ctx.getNDManager().create(input));
            return list;
        }
    }
} 