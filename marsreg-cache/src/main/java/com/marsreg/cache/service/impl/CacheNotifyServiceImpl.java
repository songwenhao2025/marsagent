package com.marsreg.cache.service.impl;

import com.marsreg.cache.config.NotifyConfig;
import com.marsreg.cache.service.CacheNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheNotifyServiceImpl implements CacheNotifyService {

    private final NotifyConfig notifyConfig;
    private final RestTemplate restTemplate;
    
    private JavaMailSender mailSender;

    @Override
    public void notify(String type, String title, String content, NotifyLevel level) {
        if (!notifyConfig.isEnabled()) {
            return;
        }

        switch (notifyConfig.getType()) {
            case "log":
                notifyByLog(type, title, content, level);
                break;
            case "email":
                notifyByEmail(type, title, content, level);
                break;
            case "webhook":
                notifyByWebhook(type, title, content, level);
                break;
            default:
                log.warn("Unknown notify type: {}", notifyConfig.getType());
        }
    }

    private void notifyByLog(String type, String title, String content, NotifyLevel level) {
        String message = String.format("[%s] %s: %s - %s", level, type, title, content);
        switch (level) {
            case INFO:
                log.info(message);
                break;
            case WARN:
                log.warn(message);
                break;
            case ERROR:
                log.error(message);
                break;
        }
    }

    private void notifyByEmail(String type, String title, String content, NotifyLevel level) {
        try {
            MimeMessage message = getMailSender().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(notifyConfig.getEmail().getFrom());
            helper.setTo(notifyConfig.getEmail().getTo());
            helper.setSubject(String.format("[%s] %s: %s", level, type, title));
            helper.setText(content);
            
            getMailSender().send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email notification", e);
        }
    }

    private void notifyByWebhook(String type, String title, String content, NotifyLevel level) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", type);
            payload.put("title", title);
            payload.put("content", content);
            payload.put("level", level);
            
            restTemplate.postForEntity(
                notifyConfig.getWebhook().getUrl(),
                payload,
                String.class
            );
        } catch (Exception e) {
            log.error("Failed to send webhook notification", e);
        }
    }

    private synchronized JavaMailSender getMailSender() {
        if (mailSender == null) {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(notifyConfig.getEmail().getHost());
            sender.setPort(notifyConfig.getEmail().getPort());
            sender.setUsername(notifyConfig.getEmail().getUsername());
            sender.setPassword(notifyConfig.getEmail().getPassword());
            
            Properties props = sender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            if (notifyConfig.getEmail().isSsl()) {
                props.put("mail.smtp.ssl.enable", "true");
            }
            
            mailSender = sender;
        }
        return mailSender;
    }
} 