package com.marsreg.vector.service;

import com.marsreg.vector.config.TestConfig;
import com.marsreg.vector.model.SearchResult;
import com.marsreg.vector.service.impl.HuggingFaceVectorizationService;
import com.marsreg.vector.service.impl.MilvusVectorStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")
public class VectorServiceTest {

    @Autowired
    private VectorizationService vectorizationService;

    @Autowired
    private VectorStorageService vectorStorageService;

    private float[] testVector1;
    private float[] testVector2;

    @BeforeEach
    void setUp() {
        // 生成测试向量
        testVector1 = vectorizationService.vectorize("这是一个测试文本");
        testVector2 = vectorizationService.vectorize("这是另一个测试文本");
    }

    @Test
    void testVectorization() {
        // 测试单个文本向量化
        float[] vector = vectorizationService.vectorize("测试文本");
        assertNotNull(vector);
        assertEquals(384, vector.length);

        // 测试批量向量化
        List<String> texts = Arrays.asList("文本1", "文本2", "文本3");
        List<float[]> vectors = vectorizationService.batchVectorize(texts);
        assertEquals(3, vectors.size());
        assertEquals(384, vectors.get(0).length);
    }

    @Test
    void testSimilarityCalculation() {
        // 确保向量已经归一化
        float[] normalizedVector1 = vectorizationService.normalize(testVector1);
        float[] normalizedVector2 = vectorizationService.normalize(testVector2);
        
        float similarity = vectorizationService.calculateSimilarity(normalizedVector1, normalizedVector2);
        System.out.println("相似度: " + similarity); // 添加调试输出
        assertTrue(similarity >= -1 && similarity <= 1, "相似度应该在-1到1之间");
        
        // 测试相同向量的相似度
        float selfSimilarity = vectorizationService.calculateSimilarity(normalizedVector1, normalizedVector1);
        assertEquals(1.0f, selfSimilarity, 0.0001f, "相同向量的相似度应该为1");
        
        // 测试正交向量的相似度
        float[] orthogonalVector = new float[normalizedVector1.length];
        Arrays.fill(orthogonalVector, 0.0f);
        orthogonalVector[0] = 1.0f; // 确保向量不为零
        orthogonalVector = vectorizationService.normalize(orthogonalVector); // 归一化正交向量
        float orthogonalSimilarity = vectorizationService.calculateSimilarity(normalizedVector1, orthogonalVector);
        assertTrue(orthogonalSimilarity >= -1 && orthogonalSimilarity <= 1, "正交向量的相似度应该在-1到1之间");
    }

    @Test
    void testVectorStorage() {
        // 测试存储单个向量
        String id1 = "test1";
        vectorStorageService.store(id1, testVector1);
        
        // 测试批量存储
        Map<String, float[]> vectors = new HashMap<>();
        vectors.put("test2", testVector2);
        vectorStorageService.batchStore(vectors);

        // 等待索引刷新
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 测试向量检索
        List<Map.Entry<String, Float>> results = vectorStorageService.search(testVector1, 2, 0.5f);
        assertNotNull(results, "搜索结果不应为null");
        assertFalse(results.isEmpty(), "搜索结果不应为空");
        assertTrue(results.size() <= 2, "搜索结果数量不应超过2");
        
        // 验证搜索结果
        Map.Entry<String, Float> firstResult = results.get(0);
        assertNotNull(firstResult, "第一个搜索结果不应为null");
        assertNotNull(firstResult.getKey(), "搜索结果ID不应为null");
        assertTrue(firstResult.getValue() >= 0 && firstResult.getValue() <= 1, "相似度分数应在0-1之间");

        // 测试更新向量
        float[] newVector = vectorizationService.vectorize("更新后的测试文本");
        vectorStorageService.updateVector(id1, newVector);

        // 等待索引刷新
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 测试删除向量
        vectorStorageService.delete(id1);
    }

    @Test
    void testVectorNormalization() {
        float[] normalized = vectorizationService.normalize(testVector1);
        float sum = 0;
        for (float v : normalized) {
            sum += v * v;
        }
        assertEquals(1.0, sum, 0.0001);
    }
} 