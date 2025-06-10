package com.marsreg.document.service.impl;

import com.marsreg.common.exception.BusinessException;
import com.marsreg.document.config.CacheConfig;
import com.marsreg.document.config.ChunkingConfig;
import com.marsreg.document.entity.DocumentEntity;
import com.marsreg.document.entity.DocumentChunk;
import com.marsreg.document.entity.DocumentChunkMetadata;
import com.marsreg.document.enums.DocumentStatus;
import com.marsreg.document.repository.DocumentChunkRepository;
import com.marsreg.document.service.DocumentChunkMetadataService;
import com.marsreg.document.service.DocumentIndexService;
import com.marsreg.document.service.DocumentProcessService;
import com.marsreg.document.service.DocumentService;
import com.marsreg.document.service.DocumentStorageService;
import com.marsreg.document.service.DocumentVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessServiceImpl implements DocumentProcessService {

    private final DocumentStorageService documentStorageService;
    private final DocumentIndexService documentIndexService;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentChunkMetadataService metadataService;
    private final DocumentVectorService documentVectorService;
    private final ChunkingConfig chunkingConfig;
    private final DocumentService documentService;
    private final Tika tika = new Tika();

    // 语言代码映射
    private static final Map<String, String> LANGUAGE_CODE_MAP = new HashMap<>();
    static {
        LANGUAGE_CODE_MAP.put("zh", "zh-CN");
        LANGUAGE_CODE_MAP.put("en", "en-US");
        LANGUAGE_CODE_MAP.put("ja", "ja-JP");
        LANGUAGE_CODE_MAP.put("ko", "ko-KR");
        LANGUAGE_CODE_MAP.put("fr", "fr-FR");
        LANGUAGE_CODE_MAP.put("de", "de-DE");
        LANGUAGE_CODE_MAP.put("es", "es-ES");
        LANGUAGE_CODE_MAP.put("ru", "ru-RU");
    }

    @Override
    @Transactional
    public DocumentEntity process(DocumentEntity document) {
        try {
            document.setStatus(DocumentStatus.PROCESSING);
            documentService.save(document);

            // 提取文本
            String text = extractText(document);
            document.setContent(text);

            // 生成向量
            documentVectorService.generateVector(document);

            document.setStatus(DocumentStatus.COMPLETED);
            documentService.save(document);
            return document;
        } catch (Exception e) {
            document.setStatus(DocumentStatus.FAILED);
            documentService.save(document);
            throw new RuntimeException("Failed to process document", e);
        }
    }

    @Override
    public void processBatch(List<DocumentEntity> documents) {
        for (DocumentEntity document : documents) {
            process(document);
        }
    }

    @Override
    public String processDocument(DocumentEntity document) {
        return extractText(document);
    }

    @Override
    public String extractText(DocumentEntity document) {
        try {
            var inputStream = documentStorageService.getDocument(document);
            var reader = new BufferedReader(new InputStreamReader(inputStream));
            var text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append("\n");
            }
            return text.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from document", e);
        }
    }

    @Override
    public String cleanText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 1. 移除特殊字符
        text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        
        // 2. 移除多余的空白字符
        text = text.replaceAll("\\s+", " ");
        
        // 3. 移除HTML标签
        text = text.replaceAll("<[^>]*>", "");
        
        // 4. 移除URL
        text = text.replaceAll("https?://\\S+\\s?", "");
        
        // 5. 移除邮箱
        text = text.replaceAll("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b", "");
        
        // 6. 移除重复的标点符号
        text = text.replaceAll("([.,!?])\\1+", "$1");
        
        // 7. 移除行号
        text = text.replaceAll("^\\d+\\.\\s*", "");
        
        // 8. 移除页眉页脚
        text = text.replaceAll("Page \\d+ of \\d+", "");
        
        // 9. 规范化换行符
        text = text.replaceAll("\\r\\n|\\r", "\n");
        
        // 10. 移除空行
        text = text.replaceAll("(?m)^\\s*$[\n\r]{1,}", "\n");
        
        return text.trim();
    }

    @Override
    public String detectLanguage(String text) {
        if (text == null || text.isEmpty()) {
            return "unknown";
        }

        try {
            // 使用Tika的语言检测
            String language = tika.detect(text);

            // 如果置信度太低，使用启发式规则
            if (language == null || language.equals("unknown")) {
                language = detectLanguageByRules(text);
            }

            // 转换为标准语言代码
            return LANGUAGE_CODE_MAP.getOrDefault(language, language);
        } catch (Exception e) {
            log.warn("语言检测失败，使用启发式规则", e);
            return detectLanguageByRules(text);
        }
    }

    /**
     * 使用启发式规则检测语言
     */
    private String detectLanguageByRules(String text) {
        // 1. 检查中文字符
        if (text.matches(".*[\\u4e00-\\u9fa5].*")) {
            return "zh-CN";
        }

        // 2. 检查日文假名和汉字
        if (text.matches(".*[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FFF].*")) {
            return "ja-JP";
        }

        // 3. 检查韩文
        if (text.matches(".*[\\uAC00-\\uD7AF\\u1100-\\u11FF].*")) {
            return "ko-KR";
        }

        // 4. 检查俄文
        if (text.matches(".*[\\u0400-\\u04FF].*")) {
            return "ru-RU";
        }

        // 5. 检查常见英文单词
        if (text.matches(".*\\b(the|and|of|to|a|in|that|have|I|it|for|not|on|with|he|as|you|do|at|this|but|his|by|from|they|we|say|her|she|or|an|will|my|one|all|would|there|their|what|so|up|out|if|about|who|get|which|go|me)\\b.*")) {
            return "en-US";
        }

        // 6. 检查常见法文单词
        if (text.matches(".*\\b(le|la|les|un|une|des|et|est|dans|pour|que|qui|ce|cette|ces|mon|ton|son|notre|votre|leur)\\b.*")) {
            return "fr-FR";
        }

        // 7. 检查常见德文单词
        if (text.matches(".*\\b(der|die|das|und|ist|in|den|von|zu|mit|sich|auf|für|ist|nicht|ein|eine|eines|einem|einen|einer|ein|als|auch|es|an|auch|auf|aus|bei|nach|seit|von|zu|zum|zur|für|ohne|um|durch|gegen|wider|entlang|binnen|innerhalb|außerhalb|oberhalb|unterhalb|diesseits|jenseits|beiderseits|abseits|längs|unweit|rechts|links|abzüglich|zuzüglich|einschließlich|ausschließlich|unbeschadet|anlässlich|aufgrund|kraft|laut|gemäß|zufolge|halber|wegen|während|trotz|statt|anstelle|anhand|mithilfe|vermöge|zugunsten|zuungunsten|zulasten|zugunsten|zuliebe|zuwider|entgegen|gegenüber|neben|bei|mit|nach|seit|von|aus|bei|mit|nach|seit|von|zu|zum|zur|für|ohne|um|durch|gegen|wider|entlang|binnen|innerhalb|außerhalb|oberhalb|unterhalb|diesseits|jenseits|beiderseits|abseits|längs|unweit|rechts|links|abzüglich|zuzüglich|einschließlich|ausschließlich|unbeschadet|anlässlich|aufgrund|kraft|laut|gemäß|zufolge|halber|wegen|während|trotz|statt|anstelle|anhand|mithilfe|vermöge|zugunsten|zuungunsten|zulasten|zugunsten|zuliebe|zuwider|entgegen|gegenüber|neben)\\b.*")) {
            return "de-DE";
        }

        // 8. 检查常见西班牙文单词
        if (text.matches(".*\\b(el|la|los|las|un|una|unos|unas|y|es|en|para|que|quien|cual|cuyo|cuyos|cuyas|cuyas|mi|tu|su|nuestro|vuestro|su)\\b.*")) {
            return "es-ES";
        }

        return "unknown";
    }

    @Override
    @Transactional
    public List<String> chunkText(String text, int chunkSize, int overlapSize) {
        if (text == null || text.isEmpty() || chunkSize <= 0) {
            return List.of();
        }

        // 使用配置的默认值
        int actualChunkSize = chunkSize > 0 ? chunkSize : chunkingConfig.getDefaultMaxChunkSize();
        int actualOverlapSize = overlapSize > 0 ? overlapSize : chunkingConfig.getDefaultOverlapSize();

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int textLength = text.length();
        int chunkIndex = 0;

        while (start < textLength) {
            int end = Math.min(start + actualChunkSize, textLength);
            
            // 如果不是最后一个分块，尝试在句子边界处分割
            if (end < textLength) {
                end = findSentenceBoundary(text, start, end);
            }
            
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            
            // 计算下一个分块的起始位置，考虑重叠
            start = end - actualOverlapSize;
            chunkIndex++;
        }

        return chunks;
    }

    @Override
    @Transactional
    public List<String> smartChunkText(String text, int maxChunkSize, int minChunkSize) {
        if (text == null || text.isEmpty() || maxChunkSize <= 0 || minChunkSize <= 0) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int textLength = text.length();

        while (start < textLength) {
            int end = Math.min(start + maxChunkSize, textLength);
            
            // 如果不是最后一个分块，尝试在段落边界处分割
            if (end < textLength) {
                end = findParagraphBoundary(text, start, end);
            }
            
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty() && chunk.length() >= minChunkSize) {
                chunks.add(chunk);
            }
            
            start = end;
        }

        return chunks;
    }

    private int findSentenceBoundary(String text, int start, int end) {
        // 在最大分块大小范围内查找句子边界
        int boundary = end;
        for (int i = end - 1; i >= start; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '。' || c == '！' || c == '？') {
                boundary = i + 1;
                break;
            }
        }
        return boundary;
    }

    private int findParagraphBoundary(String text, int start, int end) {
        // 在最大分块大小范围内查找段落边界
        int boundary = end;
        for (int i = end - 1; i >= start; i--) {
            if (text.charAt(i) == '\n' && (i == 0 || text.charAt(i - 1) == '\n')) {
                boundary = i;
                break;
            }
        }
        return boundary;
    }

    @Override
    @Transactional
    public void saveChunks(Long documentId, List<String> chunks, String language) {
        DocumentEntity document = documentService.getDocumentEntity(documentId)
            .orElseThrow(() -> new BusinessException("文档不存在"));

        // 删除现有的分块
        documentChunkRepository.deleteByDocumentId(documentId);

        // 保存新的分块
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(document);
            chunk.setContent(chunks.get(i));
            chunk.setChunkIndex(i);
            chunk.setLanguage(language);
            documentChunkRepository.save(chunk);

            // 保存分块元数据
            saveChunkMetadata(chunk);
        }
    }

    private void saveChunkMetadata(DocumentChunk chunk) {
        String text = chunk.getContent();
        
        // 保存基本统计信息
        metadataService.save(createMetadata(chunk, "word_count", String.valueOf(countWords(text)), "number", "单词数"));
        metadataService.save(createMetadata(chunk, "sentence_count", String.valueOf(countSentences(text)), "number", "句子数"));
        metadataService.save(createMetadata(chunk, "paragraph_count", String.valueOf(countParagraphs(text)), "number", "段落数"));
        metadataService.save(createMetadata(chunk, "avg_sentence_length", String.valueOf(calculateAvgSentenceLength(text)), "number", "平均句子长度"));
    }

    private DocumentChunkMetadata createMetadata(DocumentChunk chunk, String key, String value, String type, String description) {
        DocumentChunkMetadata metadata = new DocumentChunkMetadata();
        metadata.setDocumentId(chunk.getDocument().getId());
        metadata.setChunkId(chunk.getId());
        metadata.setKey(key);
        metadata.setValue(value);
        metadata.setType(type);
        metadata.setDescription(description);
        return metadata;
    }

    private double calculateAvgSentenceLength(String text) {
        int words = countWords(text);
        int sentences = countSentences(text);
        return sentences > 0 ? (double) words / sentences : 0;
    }

    @Override
    @Cacheable(value = CacheConfig.DOCUMENT_CHUNKS_CACHE, key = "#documentId")
    public List<String> getChunks(Long documentId) {
        return documentChunkRepository.findByDocumentIdOrderByChunkIndex(documentId)
            .stream()
            .map(DocumentChunk::getContent)
            .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = CacheConfig.DOCUMENT_CHUNKS_CACHE, key = "#documentId")
    public void clearChunksCache(Long documentId) {
        // 缓存清除由注解处理
    }

    @Override
    @CacheEvict(value = {CacheConfig.DOCUMENT_CHUNKS_CACHE, CacheConfig.DOCUMENT_CONTENT_CACHE}, allEntries = true)
    public void clearDocumentCache(Long documentId) {
        // 缓存清除由注解处理
    }

    private int countWords(String text) {
        return text.split("\\s+").length;
    }

    private int countSentences(String text) {
        return text.split("[.!?。！？]").length;
    }

    private int countParagraphs(String text) {
        return text.split("\\n\\s*\\n").length;
    }

    @Override
    public String extractText(InputStream inputStream, String fileName) {
        try {
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            
            BodyContentHandler handler = new BodyContentHandler(-1);
            ParseContext context = new ParseContext();
            Parser parser = new AutoDetectParser();
            
            parser.parse(inputStream, handler, metadata, context);
            return handler.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from document", e);
        }
    }

    private List<DocumentChunk> splitIntoChunks(String text) {
        List<String> chunks = smartChunkText(text, chunkingConfig.getDefaultMaxChunkSize(), chunkingConfig.getDefaultMinChunkSize());
        List<DocumentChunk> documentChunks = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setContent(chunks.get(i));
            chunk.setChunkIndex(i);
            documentChunks.add(chunk);
        }
        
        return documentChunks;
    }
} 