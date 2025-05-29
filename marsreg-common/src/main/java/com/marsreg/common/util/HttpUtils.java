package com.marsreg.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HTTP请求工具类
 */
@Slf4j
public class HttpUtils {
    
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int SOCKET_TIMEOUT = 10000;
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
        .setConnectTimeout(CONNECT_TIMEOUT)
        .setSocketTimeout(SOCKET_TIMEOUT)
        .build();
    
    private HttpUtils() {
        throw new IllegalStateException("Utility class");
    }
    
    /**
     * 发送GET请求
     */
    public static String get(String url) throws IOException {
        return get(url, null);
    }
    
    /**
     * 发送GET请求（带参数）
     */
    public static String get(String url, Map<String, String> params) throws IOException {
        if (params != null && !params.isEmpty()) {
            StringBuilder queryString = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (queryString.length() > 0) {
                    queryString.append("&");
                }
                queryString.append(entry.getKey()).append("=").append(entry.getValue());
            }
            url = url + (url.contains("?") ? "&" : "?") + queryString;
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setConfig(REQUEST_CONFIG);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        }
    }
    
    /**
     * 发送GET请求（带参数和请求头）
     */
    public static String get(String url, Map<String, String> headers, Map<String, String> params) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setConfig(REQUEST_CONFIG);
            
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpGet.setHeader(entry.getKey(), entry.getValue());
                }
            }
            
            if (params != null && !params.isEmpty()) {
                StringBuilder queryString = new StringBuilder();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (queryString.length() > 0) {
                        queryString.append("&");
                    }
                    queryString.append(entry.getKey()).append("=").append(entry.getValue());
                }
                try {
                    httpGet.setURI(new java.net.URI(url + (url.contains("?") ? "&" : "?") + queryString));
                } catch (java.net.URISyntaxException e) {
                    throw new IOException("Invalid URL: " + url, e);
                }
            }
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        }
    }
    
    /**
     * 发送POST请求（JSON数据）
     */
    public static String postJson(String url, String json) throws IOException {
        return postJson(url, json, null);
    }
    
    /**
     * 发送POST请求（JSON数据，带请求头）
     */
    public static String postJson(String url, String json, Map<String, String> headers) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(REQUEST_CONFIG);
            
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }
            }
            
            StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        }
    }
    
    /**
     * 发送POST请求（表单数据）
     */
    public static String postForm(String url, Map<String, String> params) throws IOException {
        return postForm(url, params, null);
    }
    
    /**
     * 发送POST请求（表单数据，带请求头）
     */
    public static String postForm(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(REQUEST_CONFIG);
            
            if (params != null) {
                List<NameValuePair> parameters = new ArrayList<>();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    parameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
                httpPost.setEntity(new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8));
            }
            
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }
            }
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        }
    }
    
    /**
     * 发送POST请求（文件上传）
     */
    public static String uploadFile(String url, File file, String fileFieldName, Map<String, String> formData) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(REQUEST_CONFIG);
            
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody(fileFieldName, file);
            
            if (formData != null) {
                for (Map.Entry<String, String> entry : formData.entrySet()) {
                    builder.addTextBody(entry.getKey(), entry.getValue());
                }
            }
            
            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        }
    }
    
    /**
     * 发送PUT请求（JSON数据）
     */
    public static String putJson(String url, String json) throws IOException {
        return putJson(url, json, null);
    }
    
    /**
     * 发送PUT请求（JSON数据，带请求头）
     */
    public static String putJson(String url, String json, Map<String, String> headers) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPut httpPut = new HttpPut(url);
            httpPut.setConfig(REQUEST_CONFIG);
            
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpPut.setHeader(entry.getKey(), entry.getValue());
                }
            }
            
            StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
            httpPut.setEntity(entity);
            
            try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        }
    }
    
    /**
     * 发送DELETE请求
     */
    public static String delete(String url) throws IOException {
        return delete(url, null);
    }
    
    /**
     * 发送DELETE请求（带请求头）
     */
    public static String delete(String url, Map<String, String> headers) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpDelete httpDelete = new HttpDelete(url);
            httpDelete.setConfig(REQUEST_CONFIG);
            
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpDelete.setHeader(entry.getKey(), entry.getValue());
                }
            }
            
            try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        }
    }
    
    /**
     * 下载文件
     */
    public static File downloadFile(String url, String targetPath) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setConfig(REQUEST_CONFIG);
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    File file = new File(targetPath);
                    try (InputStream inputStream = entity.getContent();
                         FileOutputStream outputStream = new FileOutputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                    return file;
                }
            }
        }
        return null;
    }
} 