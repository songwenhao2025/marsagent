package com.marsreg.common.util;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 验证工具类
 */
public class ValidateUtils {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern URL_PATTERN = Pattern.compile("^(http|https)://[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,}(?:/[^/]*)*$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$");
    private static final Pattern IPV4_PATTERN = Pattern.compile("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    private static final Pattern CHINESE_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5]+$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^[0-9]+$");
    private static final Pattern LETTER_PATTERN = Pattern.compile("^[a-zA-Z]+$");
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    
    /**
     * 判断对象是否为空
     */
    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        }
        if (obj instanceof String) {
            return ((String) obj).trim().isEmpty();
        }
        if (obj instanceof Collection) {
            return ((Collection<?>) obj).isEmpty();
        }
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).isEmpty();
        }
        if (obj instanceof Object[]) {
            return ((Object[]) obj).length == 0;
        }
        return false;
    }
    
    /**
     * 判断对象是否不为空
     */
    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }
    
    /**
     * 判断是否为邮箱
     */
    public static boolean isEmail(String str) {
        return str != null && EMAIL_PATTERN.matcher(str).matches();
    }
    
    /**
     * 判断是否为手机号
     */
    public static boolean isPhone(String str) {
        return str != null && PHONE_PATTERN.matcher(str).matches();
    }
    
    /**
     * 判断是否为URL
     */
    public static boolean isUrl(String str) {
        return str != null && URL_PATTERN.matcher(str).matches();
    }
    
    /**
     * 判断是否为身份证号
     */
    public static boolean isIdCard(String str) {
        return str != null && ID_CARD_PATTERN.matcher(str).matches();
    }
    
    /**
     * 判断是否为IPv4地址
     */
    public static boolean isIpv4(String str) {
        return str != null && IPV4_PATTERN.matcher(str).matches();
    }
    
    /**
     * 判断是否为中文
     */
    public static boolean isChinese(String str) {
        return str != null && CHINESE_PATTERN.matcher(str).matches();
    }
    
    /**
     * 判断是否为数字
     */
    public static boolean isNumber(String str) {
        return str != null && NUMBER_PATTERN.matcher(str).matches();
    }
    
    /**
     * 判断是否为字母
     */
    public static boolean isLetter(String str) {
        return str != null && LETTER_PATTERN.matcher(str).matches();
    }
    
    /**
     * 判断是否为字母数字组合
     */
    public static boolean isAlphanumeric(String str) {
        return str != null && ALPHANUMERIC_PATTERN.matcher(str).matches();
    }
    
    /**
     * 判断字符串长度是否在指定范围内
     */
    public static boolean isLengthBetween(String str, int min, int max) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        return length >= min && length <= max;
    }
    
    /**
     * 判断数字是否在指定范围内
     */
    public static boolean isBetween(int num, int min, int max) {
        return num >= min && num <= max;
    }
    
    /**
     * 判断数字是否在指定范围内
     */
    public static boolean isBetween(long num, long min, long max) {
        return num >= min && num <= max;
    }
    
    /**
     * 判断数字是否在指定范围内
     */
    public static boolean isBetween(double num, double min, double max) {
        return num >= min && num <= max;
    }
    
    /**
     * 判断是否为有效的端口号
     */
    public static boolean isValidPort(int port) {
        return port > 0 && port < 65536;
    }
    
    /**
     * 判断是否为有效的文件扩展名
     */
    public static boolean isValidFileExtension(String extension) {
        if (extension == null) {
            return false;
        }
        String[] validExtensions = {"jpg", "jpeg", "png", "gif", "bmp", "pdf", "doc", "docx", "xls", "xlsx", "txt"};
        extension = extension.toLowerCase();
        for (String validExtension : validExtensions) {
            if (validExtension.equals(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 判断是否为有效的文件大小（单位：字节）
     */
    public static boolean isValidFileSize(long size, long maxSize) {
        return size > 0 && size <= maxSize;
    }
    
    /**
     * 判断是否为有效的日期格式（yyyy-MM-dd）
     */
    public static boolean isValidDateFormat(String date) {
        if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }
        try {
            String[] parts = date.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            
            if (year < 1900 || year > 9999) {
                return false;
            }
            if (month < 1 || month > 12) {
                return false;
            }
            if (day < 1 || day > 31) {
                return false;
            }
            
            // 检查月份天数
            int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
            if (month == 2 && isLeapYear(year)) {
                return day <= 29;
            }
            return day <= daysInMonth[month - 1];
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 判断是否为闰年
     */
    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }
    
    /**
     * 判断是否为有效的时间格式（HH:mm:ss）
     */
    public static boolean isValidTimeFormat(String time) {
        if (time == null || !time.matches("\\d{2}:\\d{2}:\\d{2}")) {
            return false;
        }
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            int second = Integer.parseInt(parts[2]);
            
            return hour >= 0 && hour <= 23 &&
                   minute >= 0 && minute <= 59 &&
                   second >= 0 && second <= 59;
        } catch (Exception e) {
            return false;
        }
    }
} 