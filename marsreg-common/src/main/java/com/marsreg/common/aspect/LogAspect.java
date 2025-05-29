package com.marsreg.common.aspect;

import com.marsreg.common.annotation.Log;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

/**
 * 日志切面
 * 处理 @Log 注解的切面类
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    @Around("@annotation(com.marsreg.common.annotation.Log)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;

        try {
            result = point.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            saveLog(point, result, exception, startTime);
        }
    }

    private void saveLog(ProceedingJoinPoint point, Object result, Exception exception, long startTime) {
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            Method method = signature.getMethod();
            Log logAnnotation = method.getAnnotation(Log.class);

            if (logAnnotation == null) {
                return;
            }

            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

            // 构建日志信息
            StringBuilder logInfo = new StringBuilder();
            logInfo.append("\n===================== 操作日志 =====================\n");
            logInfo.append("模块名称: ").append(logAnnotation.module()).append("\n");
            logInfo.append("操作类型: ").append(logAnnotation.operation()).append("\n");
            logInfo.append("操作描述: ").append(logAnnotation.description()).append("\n");

            if (request != null) {
                logInfo.append("请求URL: ").append(request.getRequestURL()).append("\n");
                logInfo.append("请求方式: ").append(request.getMethod()).append("\n");
                logInfo.append("IP地址: ").append(getIpAddress(request)).append("\n");
            }

            if (logAnnotation.saveRequestData()) {
                logInfo.append("请求参数: ").append(Arrays.toString(point.getArgs())).append("\n");
            }

            if (logAnnotation.saveResponseData() && result != null) {
                logInfo.append("响应结果: ").append(result).append("\n");
            }

            if (logAnnotation.saveExecutionTime()) {
                long executionTime = System.currentTimeMillis() - startTime;
                logInfo.append("执行时间: ").append(executionTime).append("ms\n");
            }

            if (logAnnotation.saveException() && exception != null) {
                logInfo.append("异常信息: ").append(exception.getMessage()).append("\n");
                logInfo.append("异常堆栈: ").append(Arrays.toString(exception.getStackTrace())).append("\n");
            }

            logInfo.append("操作时间: ").append(LocalDateTime.now()).append("\n");
            logInfo.append("==================================================");

            // 记录日志
            if (exception != null) {
                log.error(logInfo.toString());
            } else {
                log.info(logInfo.toString());
            }
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
} 