package com.marsreg.inference.service.impl;

import com.marsreg.inference.service.LLMService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService implements LLMService {

    @Value("${llm.model}")
    private String model;

    @Value("${llm.temperature}")
    private Double temperature;

    @Value("${llm.max-tokens}")
    private Integer maxTokens;

    @Value("${llm.timeout}")
    private Integer timeout;

    private final OpenAiService openAiService;

    @Override
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public String generateAnswer(String prompt, List<String> context, Map<String, Object> parameters) {
        List<ChatMessage> messages = buildMessages(prompt, context);
        
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model(model)
            .messages(messages)
            .temperature(temperature)
            .maxTokens(maxTokens)
            .build();
            
        return openAiService.createChatCompletion(request)
            .getChoices().get(0).getMessage().getContent();
    }

    @Override
    public void generateAnswerStream(String prompt, List<String> context, Map<String, Object> parameters,
                                   StreamCallback callback) {
        List<ChatMessage> messages = buildMessages(prompt, context);
        
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model(model)
            .messages(messages)
            .temperature(temperature)
            .maxTokens(maxTokens)
            .stream(true)
            .build();
            
        openAiService.streamChatCompletion(request)
            .doOnNext(response -> {
                String content = response.getChoices().get(0).getMessage().getContent();
                if (content != null) {
                    callback.onToken(content);
                }
            })
            .doOnComplete(callback::onComplete)
            .doOnError(callback::onError)
            .subscribe();
    }

    private List<ChatMessage> buildMessages(String prompt, List<String> context) {
        List<ChatMessage> messages = new ArrayList<>();
        
        // 添加系统消息
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
            "你是一个专业的AI助手，基于给定的上下文回答问题。"));
            
        // 添加上下文
        if (context != null && !context.isEmpty()) {
            String contextText = context.stream()
                .collect(Collectors.joining("\n\n"));
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "以下是相关上下文：\n" + contextText));
        }
        
        // 添加用户问题
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));
        
        return messages;
    }
} 