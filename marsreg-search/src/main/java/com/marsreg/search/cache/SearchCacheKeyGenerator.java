package com.marsreg.search.cache;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class SearchCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        // 生成缓存键：方法名 + 参数值
        String methodName = method.getName();
        String paramString = Arrays.stream(params)
                .map(param -> {
                    if (param == null) {
                        return "null";
                    }
                    if (param instanceof String) {
                        return (String) param;
                    }
                    return param.toString();
                })
                .collect(Collectors.joining("_"));
        
        return methodName + ":" + paramString;
    }
} 