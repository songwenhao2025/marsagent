package com.marsreg.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

/**
 * 日期时间工具类
 */
public class DateUtils {
    
    public static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final String TIMESTAMP_PATTERN = "yyyyMMddHHmmssSSS";
    
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    
    /**
     * 获取当前时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
    
    /**
     * 获取当前日期
     */
    public static LocalDate today() {
        return LocalDate.now();
    }
    
    /**
     * 格式化日期时间
     */
    public static String format(LocalDateTime dateTime) {
        return format(dateTime, DEFAULT_PATTERN);
    }
    
    /**
     * 格式化日期时间
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }
    
    /**
     * 解析日期时间字符串
     */
    public static LocalDateTime parse(String dateTimeStr) {
        return parse(dateTimeStr, DEFAULT_PATTERN);
    }
    
    /**
     * 解析日期时间字符串
     */
    public static LocalDateTime parse(String dateTimeStr, String pattern) {
        if (dateTimeStr == null) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
    }
    
    /**
     * Date转LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZONE_ID);
    }
    
    /**
     * LocalDateTime转Date
     */
    public static Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Date.from(dateTime.atZone(ZONE_ID).toInstant());
    }
    
    /**
     * 获取指定日期的开始时间
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }
    
    /**
     * 获取指定日期的结束时间
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(23, 59, 59, 999999999);
    }
    
    /**
     * 获取本月开始时间
     */
    public static LocalDateTime startOfMonth() {
        return startOfMonth(LocalDate.now());
    }
    
    /**
     * 获取指定月份的开始时间
     */
    public static LocalDateTime startOfMonth(LocalDate date) {
        return date.withDayOfMonth(1).atStartOfDay();
    }
    
    /**
     * 获取本月结束时间
     */
    public static LocalDateTime endOfMonth() {
        return endOfMonth(LocalDate.now());
    }
    
    /**
     * 获取指定月份的结束时间
     */
    public static LocalDateTime endOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59, 999999999);
    }
    
    /**
     * 计算两个日期之间的天数
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }
    
    /**
     * 计算两个日期时间之间的小时数
     */
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.HOURS.between(start, end);
    }
    
    /**
     * 计算两个日期时间之间的分钟数
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }
    
    /**
     * 获取时间戳（毫秒）
     */
    public static long timestamp() {
        return System.currentTimeMillis();
    }
    
    /**
     * 获取时间戳（秒）
     */
    public static long timestampSeconds() {
        return Instant.now().getEpochSecond();
    }
    
    /**
     * 判断是否过期
     */
    public static boolean isExpired(LocalDateTime dateTime) {
        return dateTime.isBefore(LocalDateTime.now());
    }
    
    /**
     * 判断是否在指定时间范围内
     */
    public static boolean isBetween(LocalDateTime dateTime, LocalDateTime start, LocalDateTime end) {
        return dateTime.isAfter(start) && dateTime.isBefore(end);
    }
} 