package com.marsreg.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JSON处理工具类
 */
@Slf4j
public class JsonUtils {
    
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    /**
     * 对象转JSON字符串
     */
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("对象转JSON字符串失败", e);
            return null;
        }
    }
    
    /**
     * 对象转JSON字符串（格式化）
     */
    public static String toPrettyJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("对象转JSON字符串失败", e);
            return null;
        }
    }
    
    /**
     * JSON字符串转对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            log.error("JSON字符串转对象失败", e);
            return null;
        }
    }
    
    /**
     * JSON字符串转对象（支持泛型）
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (IOException e) {
            log.error("JSON字符串转对象失败", e);
            return null;
        }
    }
    
    /**
     * JSON字符串转List
     */
    public static <T> List<T> toList(String json, Class<T> clazz) {
        if (StringUtils.isEmpty(json)) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            log.error("JSON字符串转List失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * JSON字符串转Map
     */
    public static <K, V> Map<K, V> toMap(String json, Class<K> keyClass, Class<V> valueClass) {
        if (StringUtils.isEmpty(json)) {
            return Collections.emptyMap();
        }
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructMapType(Map.class, keyClass, valueClass));
        } catch (IOException e) {
            log.error("JSON字符串转Map失败", e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * 对象转Map
     */
    public static Map<String, Object> toMap(Object object) {
        if (object == null) {
            return Collections.emptyMap();
        }
        try {
            return MAPPER.convertValue(object, new TypeReference<Map<String, Object>>() {});
        } catch (IllegalArgumentException e) {
            log.error("对象转Map失败", e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Map转对象
     */
    public static <T> T fromMap(Map<String, Object> map, Class<T> clazz) {
        if (map == null) {
            return null;
        }
        try {
            return MAPPER.convertValue(map, clazz);
        } catch (IllegalArgumentException e) {
            log.error("Map转对象失败", e);
            return null;
        }
    }
    
    /**
     * 判断字符串是否为有效的JSON
     */
    public static boolean isValidJson(String json) {
        if (StringUtils.isEmpty(json)) {
            return false;
        }
        try {
            MAPPER.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 获取JSON字符串中的指定字段值
     */
    public static String getString(String json, String field) {
        if (StringUtils.isEmpty(json) || StringUtils.isEmpty(field)) {
            return null;
        }
        try {
            return MAPPER.readTree(json).get(field).asText();
        } catch (IOException e) {
            log.error("获取JSON字段值失败", e);
            return null;
        }
    }
    
    /**
     * 获取JSON字符串中的指定字段值（整数）
     */
    public static Integer getInt(String json, String field) {
        if (StringUtils.isEmpty(json) || StringUtils.isEmpty(field)) {
            return null;
        }
        try {
            return MAPPER.readTree(json).get(field).asInt();
        } catch (IOException e) {
            log.error("获取JSON字段值失败", e);
            return null;
        }
    }
    
    /**
     * 获取JSON字符串中的指定字段值（长整数）
     */
    public static Long getLong(String json, String field) {
        if (StringUtils.isEmpty(json) || StringUtils.isEmpty(field)) {
            return null;
        }
        try {
            return MAPPER.readTree(json).get(field).asLong();
        } catch (IOException e) {
            log.error("获取JSON字段值失败", e);
            return null;
        }
    }
    
    /**
     * 获取JSON字符串中的指定字段值（布尔值）
     */
    public static Boolean getBoolean(String json, String field) {
        if (StringUtils.isEmpty(json) || StringUtils.isEmpty(field)) {
            return null;
        }
        try {
            return MAPPER.readTree(json).get(field).asBoolean();
        } catch (IOException e) {
            log.error("获取JSON字段值失败", e);
            return null;
        }
    }
    
    /**
     * 获取JSON字符串中的指定字段值（对象）
     */
    public static <T> T getObject(String json, String field, Class<T> clazz) {
        if (StringUtils.isEmpty(json) || StringUtils.isEmpty(field)) {
            return null;
        }
        try {
            return MAPPER.treeToValue(MAPPER.readTree(json).get(field), clazz);
        } catch (IOException e) {
            log.error("获取JSON字段值失败", e);
            return null;
        }
    }
    
    /**
     * 获取JSON字符串中的指定字段值（List）
     */
    public static <T> List<T> getList(String json, String field, Class<T> clazz) {
        if (StringUtils.isEmpty(json) || StringUtils.isEmpty(field)) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(
                MAPPER.readTree(json).get(field).toString(),
                MAPPER.getTypeFactory().constructCollectionType(List.class, clazz)
            );
        } catch (IOException e) {
            log.error("获取JSON字段值失败", e);
            return Collections.emptyList();
        }
    }
} 