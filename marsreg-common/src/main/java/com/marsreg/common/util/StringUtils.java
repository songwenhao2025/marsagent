package com.marsreg.common.util;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 字符串处理工具类
 */
public class StringUtils {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern URL_PATTERN = Pattern.compile("^(http|https)://[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,}(?:/[^/]*)*$");
    
    /**
     * 判断字符串是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 判断字符串是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * 判断字符串是否为空白
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().length() == 0;
    }
    
    /**
     * 判断字符串是否不为空白
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    /**
     * 获取字符串长度
     */
    public static int length(String str) {
        return str == null ? 0 : str.length();
    }
    
    /**
     * 截取字符串
     */
    public static String substring(String str, int start, int end) {
        if (str == null) {
            return null;
        }
        if (end < 0) {
            end = str.length() + end;
        }
        if (start < 0) {
            start = str.length() + start;
        }
        if (end > str.length()) {
            end = str.length();
        }
        if (start > end) {
            return "";
        }
        if (start < 0) {
            start = 0;
        }
        if (end < 0) {
            end = 0;
        }
        return str.substring(start, end);
    }
    
    /**
     * 截取字符串（从开始到指定长度）
     */
    public static String left(String str, int len) {
        if (str == null) {
            return null;
        }
        if (len < 0) {
            return "";
        }
        if (str.length() <= len) {
            return str;
        }
        return str.substring(0, len);
    }
    
    /**
     * 截取字符串（从指定长度到结束）
     */
    public static String right(String str, int len) {
        if (str == null) {
            return null;
        }
        if (len < 0) {
            return "";
        }
        if (str.length() <= len) {
            return str;
        }
        return str.substring(str.length() - len);
    }
    
    /**
     * 去除字符串两端的空白字符
     */
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }
    
    /**
     * 去除字符串两端的指定字符
     */
    public static String trim(String str, String stripChars) {
        if (isEmpty(str)) {
            return str;
        }
        str = trimStart(str, stripChars);
        return trimEnd(str, stripChars);
    }
    
    /**
     * 去除字符串开头的指定字符
     */
    public static String trimStart(String str, String stripChars) {
        if (isEmpty(str)) {
            return str;
        }
        int start = 0;
        int len = str.length();
        if (stripChars == null) {
            while (start < len && Character.isWhitespace(str.charAt(start))) {
                start++;
            }
        } else {
            while (start < len && stripChars.indexOf(str.charAt(start)) != -1) {
                start++;
            }
        }
        return str.substring(start);
    }
    
    /**
     * 去除字符串结尾的指定字符
     */
    public static String trimEnd(String str, String stripChars) {
        if (isEmpty(str)) {
            return str;
        }
        int end = str.length();
        if (stripChars == null) {
            while (end > 0 && Character.isWhitespace(str.charAt(end - 1))) {
                end--;
            }
        } else {
            while (end > 0 && stripChars.indexOf(str.charAt(end - 1)) != -1) {
                end--;
            }
        }
        return str.substring(0, end);
    }
    
    /**
     * 判断字符串是否以指定前缀开始
     */
    public static boolean startsWith(String str, String prefix) {
        if (str == null || prefix == null) {
            return false;
        }
        return str.startsWith(prefix);
    }
    
    /**
     * 判断字符串是否以指定后缀结束
     */
    public static boolean endsWith(String str, String suffix) {
        if (str == null || suffix == null) {
            return false;
        }
        return str.endsWith(suffix);
    }
    
    /**
     * 判断字符串是否包含指定字符
     */
    public static boolean contains(String str, String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        return str.contains(searchStr);
    }
    
    /**
     * 判断字符串是否包含指定字符（忽略大小写）
     */
    public static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        return str.toLowerCase().contains(searchStr.toLowerCase());
    }
    
    /**
     * 判断字符串是否为邮箱
     */
    public static boolean isEmail(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(str).matches();
    }
    
    /**
     * 判断字符串是否为手机号
     */
    public static boolean isPhone(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return PHONE_PATTERN.matcher(str).matches();
    }
    
    /**
     * 判断字符串是否为URL
     */
    public static boolean isUrl(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return URL_PATTERN.matcher(str).matches();
    }
    
    /**
     * 将字符串转换为驼峰命名
     */
    public static String toCamelCase(String str) {
        if (isEmpty(str)) {
            return str;
        }
        str = str.toLowerCase();
        StringBuilder sb = new StringBuilder(str.length());
        boolean upperCase = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_') {
                upperCase = true;
            } else {
                if (upperCase) {
                    sb.append(Character.toUpperCase(c));
                    upperCase = false;
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }
    
    /**
     * 将字符串转换为下划线命名
     */
    public static String toSnakeCase(String str) {
        if (isEmpty(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        boolean upperCase = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            boolean nextUpperCase = true;
            if (i < (str.length() - 1)) {
                nextUpperCase = Character.isUpperCase(str.charAt(i + 1));
            }
            if (i > 0 && Character.isUpperCase(c)) {
                if (!upperCase || !nextUpperCase) {
                    sb.append('_');
                }
                upperCase = true;
            } else {
                upperCase = false;
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
    
    /**
     * 将字符串转换为首字母大写
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    /**
     * 将字符串转换为首字母小写
     */
    public static String uncapitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
    
    /**
     * 将字符串按指定分隔符分割
     */
    public static List<String> split(String str, String separator) {
        if (isEmpty(str)) {
            return Collections.emptyList();
        }
        return Arrays.asList(str.split(separator));
    }
    
    /**
     * 将字符串按指定分隔符分割并过滤空字符串
     */
    public static List<String> splitAndFilter(String str, String separator) {
        if (isEmpty(str)) {
            return Collections.emptyList();
        }
        return Arrays.stream(str.split(separator))
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());
    }
    
    /**
     * 将字符串列表用指定分隔符连接
     */
    public static String join(List<String> list, String separator) {
        if (list == null) {
            return null;
        }
        return String.join(separator, list);
    }
    
    /**
     * 将字符串数组用指定分隔符连接
     */
    public static String join(String[] array, String separator) {
        if (array == null) {
            return null;
        }
        return String.join(separator, array);
    }
    
    /**
     * 生成指定长度的随机字符串
     */
    public static String random(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    /**
     * 生成UUID
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
} 