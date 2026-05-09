package com.cyh.Utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Http工具类 - 支持Gzip压缩
 */
public class HttpClientUtil {

    private static final Logger log = LoggerFactory.getLogger(HttpClientUtil.class);
    static final int TIMEOUT_MSEC = 10 * 1000; // 增加超时时间到10秒

    // 使用连接池，避免每次创建新的HttpClient
    private static final CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(builderRequestConfig())
            .build();

    /**
     * 发送GET方式请求 - 支持Gzip压缩
     * @param url
     * @param paramMap
     * @return
     */
    public static String doGet(String url, Map<String, String> paramMap) {
        String result = "";
        CloseableHttpResponse response = null;

        try {
            URIBuilder builder = new URIBuilder(url);
            if (paramMap != null) {
                for (String key : paramMap.keySet()) {
                    builder.addParameter(key, paramMap.get(key));
                }
            }
            URI uri = builder.build();

            // 创建GET请求
            HttpGet httpGet = new HttpGet(uri);

            // 添加请求头，声明支持Gzip压缩
            httpGet.setHeader("Accept-Encoding", "gzip, deflate");
            httpGet.setHeader("Accept", "application/json");
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            httpGet.setHeader("X-QW-Api-Key", "");
            log.debug("发送GET请求: {}", uri);

            // 发送请求
            response = httpClient.execute(httpGet);

            // 判断响应状态
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                // 在doGet方法中，判断为Gzip响应后
                if (isGzipResponse(response)) {
                    log.debug("响应是Gzip压缩的，开始解压");
                    result = decompressGzip(response.getEntity().getContent());
                    log.debug("解压后的响应内容: {}", result);
                } else {
                    log.debug("响应未压缩，直接读取");
                    result = EntityUtils.toString(response.getEntity(), "UTF-8");
                }
                log.debug("GET请求成功，响应长度: {}", result.length());
            } else {
                log.error("GET请求失败，状态码: {}", statusCode);
                String errorBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                log.error("错误响应: {}", errorBody);
                throw new RuntimeException("HTTP请求失败，状态码: " + statusCode);
            }
        } catch (Exception e) {
            log.error("GET请求异常: {}", e.getMessage(), e);
            throw new RuntimeException("HTTP请求异常: " + e.getMessage(), e);
        } finally {
            closeResponse(response);
        }
        log.info(result);
        return result;
    }

    /**
     * 发送POST方式请求 - 支持Gzip压缩
     * @param url
     * @param paramMap
     * @return
     * @throws IOException
     */
    public static String doPost(String url, Map<String, String> paramMap) throws IOException {
        CloseableHttpResponse response = null;
        String resultString = "";

        try {
            // 创建Http Post请求
            HttpPost httpPost = new HttpPost(url);

            // 添加请求头
            httpPost.setHeader("Accept-Encoding", "gzip, deflate");
            httpPost.setHeader("Accept", "application/json");

            // 创建参数列表
            if (paramMap != null) {
                List<NameValuePair> paramList = new ArrayList<>();
                for (Map.Entry<String, String> param : paramMap.entrySet()) {
                    paramList.add(new BasicNameValuePair(param.getKey(), param.getValue()));
                }
                // 模拟表单
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(paramList, "UTF-8");
                httpPost.setEntity(entity);
            }

            log.debug("发送POST请求: {}", url);

            // 执行http请求
            response = httpClient.execute(httpPost);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                // 检查是否为Gzip压缩
                if (isGzipResponse(response)) {
                    resultString = decompressGzip(response.getEntity().getContent());
                } else {
                    resultString = EntityUtils.toString(response.getEntity(), "UTF-8");
                }
                log.debug("POST请求成功，响应长度: {}", resultString.length());
            } else {
                log.error("POST请求失败，状态码: {}", statusCode);
                String errorBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                log.error("错误响应: {}", errorBody);
                throw new RuntimeException("HTTP请求失败，状态码: " + statusCode);
            }
        } catch (Exception e) {
            log.error("POST请求异常: {}", e.getMessage(), e);
            throw new RuntimeException("HTTP请求异常: " + e.getMessage(), e);
        } finally {
            closeResponse(response);
        }

        return resultString;
    }

    /**
     * 发送POST方式请求(JSON格式) - 支持Gzip压缩
     * @param url
     * @param paramMap
     * @return
     * @throws IOException
     */
    public static String doPost4Json(String url, Map<String, String> paramMap) throws IOException {
        CloseableHttpResponse response = null;
        String resultString = "";

        try {
            // 创建Http Post请求
            HttpPost httpPost = new HttpPost(url);

            // 添加请求头
            httpPost.setHeader("Accept-Encoding", "gzip, deflate");
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");

            if (paramMap != null) {
                // 构造json格式数据
                JSONObject jsonObject = new JSONObject();
                for (Map.Entry<String, String> param : paramMap.entrySet()) {
                    jsonObject.put(param.getKey(), param.getValue());
                }
                StringEntity entity = new StringEntity(jsonObject.toString(), "UTF-8");
                httpPost.setEntity(entity);
            }

            log.debug("发送POST JSON请求: {}", url);

            // 执行http请求
            response = httpClient.execute(httpPost);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                // 检查是否为Gzip压缩
                if (isGzipResponse(response)) {
                    resultString = decompressGzip(response.getEntity().getContent());
                } else {
                    resultString = EntityUtils.toString(response.getEntity(), "UTF-8");
                }
                log.debug("POST JSON请求成功，响应长度: {}", resultString.length());
            } else {
                log.error("POST JSON请求失败，状态码: {}", statusCode);
                String errorBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                log.error("错误响应: {}", errorBody);
                throw new RuntimeException("HTTP请求失败，状态码: " + statusCode);
            }
        } catch (Exception e) {
            log.error("POST JSON请求异常: {}", e.getMessage(), e);
            throw new RuntimeException("HTTP请求异常: " + e.getMessage(), e);
        } finally {
            closeResponse(response);
        }

        return resultString;
    }


    /**
     * 检查响应是否为Gzip压缩
     */
    private static boolean isGzipResponse(CloseableHttpResponse response) {
        org.apache.http.Header contentEncoding = response.getFirstHeader("Content-Encoding");
        log.debug("Content-Encoding: {}", contentEncoding);
        return contentEncoding != null && "gzip".equalsIgnoreCase(contentEncoding.getValue());
    }

    /**
     * 解压缩Gzip数据
     */
    private static String decompressGzip(InputStream compressedStream) throws IOException {
        log.debug("开始解压Gzip数据");
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(compressedStream);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            int totalLen = 0;
            while ((len = gzipInputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, len);
                totalLen += len;
            }
            log.debug("解压完成，共读取{}字节", totalLen);
            String result = byteArrayOutputStream.toString("UTF-8");
            log.debug("解压后的字符串内容: {}", result);
            return result;
        } catch (Exception e) {
            log.error("解压Gzip数据时发生异常", e);
            throw e;
        }
    }
    /**
     * 安全关闭响应
     */
    private static void closeResponse(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException e) {
                log.warn("关闭HTTP响应异常: {}", e.getMessage());
            }
        }
    }

    /**
     * 构建请求配置
     */
    private static RequestConfig builderRequestConfig() {
        return RequestConfig.custom()
                .setConnectTimeout(TIMEOUT_MSEC)
                .setConnectionRequestTimeout(TIMEOUT_MSEC)
                .setSocketTimeout(TIMEOUT_MSEC)
                .build();
    }

    /**
     * 关闭HttpClient（在应用关闭时调用）
     */
    public static void closeHttpClient() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                log.error("关闭HttpClient异常: {}", e.getMessage(), e);
            }
        }
    }
}