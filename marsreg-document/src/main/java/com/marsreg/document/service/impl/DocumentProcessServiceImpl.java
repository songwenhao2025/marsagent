package com.marsreg.document.service.impl;

import com.marsreg.common.exception.BusinessException;
import com.marsreg.document.config.CacheConfig;
import com.marsreg.document.config.ChunkingConfig;
import com.marsreg.document.entity.Document;
import com.marsreg.document.entity.DocumentChunk;
import com.marsreg.document.entity.DocumentChunkMetadata;
import com.marsreg.document.entity.DocumentStatus;
import com.marsreg.document.repository.DocumentChunkRepository;
import com.marsreg.document.service.DocumentChunkMetadataService;
import com.marsreg.document.service.DocumentIndexService;
import com.marsreg.document.service.DocumentProcessService;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
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
    public Document process(Document document) {
        try {
            // 更新状态为处理中
            document.setStatus(DocumentStatus.PROCESSING);
            
            // 提取文本
            String text = extractText(document);
            
            // 清洗文本
            String cleanedText = cleanText(text);
            
            // 更新状态为完成
            document.setStatus(DocumentStatus.COMPLETED);
            
            return document;
        } catch (Exception e) {
            log.error("文档处理失败", e);
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            throw new BusinessException("文档处理失败: " + e.getMessage());
        }
    }

    @Override
    public String extractText(Document document) {
        try (InputStream inputStream = documentStorageService.getDocument(document)) {
            // 使用Tika提取文本
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, document.getOriginalName());
            
            BodyContentHandler handler = new BodyContentHandler(-1);
            Parser parser = new AutoDetectParser();
            ParseContext context = new ParseContext();
            
            parser.parse(inputStream, handler, metadata, context);
            
            return handler.toString();
        } catch (IOException | SAXException | TikaException e) {
            throw new BusinessException("文本提取失败", e);
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
            String chunk = text.substring(start, end);
            chunks.add(chunk);
            start = end - actualOverlapSize;
            chunkIndex++;
        }

        return chunks;
    }

    @Override
    @Transactional
    public List<String> smartChunkText(String text, int maxChunkSize, int minChunkSize) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        // 使用配置的默认值
        int actualMaxChunkSize = maxChunkSize > 0 ? maxChunkSize : chunkingConfig.getDefaultMaxChunkSize();
        int actualMinChunkSize = minChunkSize > 0 ? minChunkSize : chunkingConfig.getDefaultMinChunkSize();

        // 如果禁用智能分块，使用基本分块
        if (!chunkingConfig.isEnableSmartChunking()) {
            return chunkText(text, actualMaxChunkSize, chunkingConfig.getDefaultOverlapSize());
        }

        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\\s*\n");
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;

        for (String paragraph : paragraphs) {
            // 如果当前段落加上现有块超过最大大小，且现有块不为空
            if (currentChunk.length() + paragraph.length() > actualMaxChunkSize && currentChunk.length() > 0) {
                // 如果现有块大于最小大小，直接添加
                if (currentChunk.length() >= actualMinChunkSize) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                    chunkIndex++;
                } else if (chunkingConfig.isSplitOnSentenceBoundary()) {
                    // 否则，尝试在句子边界分割
                    String[] sentences = currentChunk.toString().split("[.!?。！？]");
                    StringBuilder tempChunk = new StringBuilder();
                    
                    for (String sentence : sentences) {
                        if (tempChunk.length() + sentence.length() > actualMaxChunkSize) {
                            if (tempChunk.length() >= actualMinChunkSize) {
                                chunks.add(tempChunk.toString().trim());
                                tempChunk = new StringBuilder();
                                chunkIndex++;
                            }
                        }
                        tempChunk.append(sentence).append(".");
                    }
                    
                    if (tempChunk.length() > 0) {
                        currentChunk = tempChunk;
                    } else {
                        currentChunk = new StringBuilder();
                    }
                }
            }
            
            // 添加当前段落
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
        }

        // 添加最后一个块
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * 保存文档分块
     * @param documentId 文档ID
     * @param chunks 分块列表
     * @param language 语言
     */
    @Transactional
    public void saveChunks(Long documentId, List<String> chunks, String language) {
        // 删除旧的分块
        documentChunkRepository.deleteByDocumentId(documentId);
        metadataService.deleteByDocumentId(documentId);

        // 保存新的分块
        List<DocumentChunk> savedChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            DocumentChunk documentChunk = new DocumentChunk();
            documentChunk.setDocumentId(documentId);
            documentChunk.setChunkIndex(i);
            documentChunk.setContent(chunk);
            documentChunk.setWordCount(countWords(chunk));
            documentChunk.setLanguage(language);
            documentChunk = documentChunkRepository.save(documentChunk);
            savedChunks.add(documentChunk);

            // 保存分块元数据
            saveChunkMetadata(documentChunk);
        }

        // 更新索引
        documentIndexService.indexDocument(new Document() {{
            setId(documentId);
        }}, chunks);

        // 向量化并存储分块
        Document document = new Document();
        document.setId(documentId);
        documentVectorService.vectorizeAndStore(document, savedChunks);
    }

    /**
     * 保存分块元数据
     * @param chunk 文档分块
     */
    private void saveChunkMetadata(DocumentChunk chunk) {
        List<DocumentChunkMetadata> metadataList = new ArrayList<>();

        // 添加基本元数据
        metadataList.add(createMetadata(chunk, "word_count", String.valueOf(chunk.getWordCount()), "number", "字数统计"));
        metadataList.add(createMetadata(chunk, "language", chunk.getLanguage(), "string", "语言"));
        metadataList.add(createMetadata(chunk, "chunk_index", String.valueOf(chunk.getChunkIndex()), "number", "分块索引"));
        metadataList.add(createMetadata(chunk, "created_at", chunk.getCreatedAt().toString(), "datetime", "创建时间"));

        // 添加内容分析元数据
        String content = chunk.getContent();
        metadataList.add(createMetadata(chunk, "sentence_count", String.valueOf(countSentences(content)), "number", "句子数量"));
        metadataList.add(createMetadata(chunk, "paragraph_count", String.valueOf(countParagraphs(content)), "number", "段落数量"));
        metadataList.add(createMetadata(chunk, "avg_sentence_length", String.valueOf(calculateAvgSentenceLength(content)), "number", "平均句子长度"));

        // 保存元数据
        metadataService.saveAll(metadataList);
    }

    /**
     * 创建元数据
     */
    private DocumentChunkMetadata createMetadata(DocumentChunk chunk, String key, String value, String type, String description) {
        DocumentChunkMetadata metadata = new DocumentChunkMetadata();
        metadata.setChunkId(chunk.getId());
        metadata.setDocumentId(chunk.getDocumentId());
        metadata.setKey(key);
        metadata.setValue(value);
        metadata.setType(type);
        metadata.setDescription(description);
        return metadata;
    }

    /**
     * 计算平均句子长度
     */
    private double calculateAvgSentenceLength(String text) {
        String[] sentences = text.split("[.!?。！？]");
        if (sentences.length == 0) {
            return 0;
        }
        int totalLength = 0;
        for (String sentence : sentences) {
            totalLength += sentence.trim().length();
        }
        return (double) totalLength / sentences.length;
    }

    /**
     * 获取文档分块
     * @param documentId 文档ID
     * @return 分块列表
     */
    @Cacheable(value = CacheConfig.DOCUMENT_CHUNKS_CACHE, key = "#documentId")
    public List<String> getChunks(Long documentId) {
        List<DocumentChunk> chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        return chunks.stream()
                .map(DocumentChunk::getContent)
                .collect(Collectors.toList());
    }

    /**
     * 清除文档分块缓存
     * @param documentId 文档ID
     */
    @CacheEvict(value = CacheConfig.DOCUMENT_CHUNKS_CACHE, key = "#documentId")
    public void clearChunksCache(Long documentId) {
        log.debug("清除文档分块缓存: {}", documentId);
    }

    /**
     * 清除文档的缓存
     * @param documentId 文档ID
     */
    @CacheEvict(value = {CacheConfig.DOCUMENT_CHUNKS_CACHE, CacheConfig.DOCUMENT_CONTENT_CACHE}, allEntries = true)
    public void clearDocumentCache(Long documentId) {
        log.debug("清除文档缓存: {}", documentId);
    }

    /**
     * 计算文本中的单词数
     */
    private int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.split("\\s+").length;
    }

    /**
     * 计算文本中的句子数
     */
    private int countSentences(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.split("[.!?。！？]").length;
    }

    /**
     * 计算文本中的段落数
     */
    private int countParagraphs(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.split("\n\\s*\n").length;
    }

    @Override
    public String extractText(InputStream inputStream, String fileName) {
        try {
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            
            BodyContentHandler handler = new BodyContentHandler(-1);
            Parser parser = new AutoDetectParser();
            ParseContext context = new ParseContext();
            
            parser.parse(inputStream, handler, metadata, context);
            
            return handler.toString();
        } catch (IOException | SAXException | TikaException e) {
            throw new BusinessException("文本提取失败", e);
        }
    }

    @Override
    public String processDocument(Document document) {
        Document processedDoc = process(document);
        return extractText(processedDoc);
    }
} 