package com.autohome.ah_grpc_plugin.utils;

import com.autohome.ah_grpc_plugin.models.ApiResult;
import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.handler.codec.http.HttpHeaders;
import org.asynchttpclient.BoundRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * 尽量使用本版本的Httpclient
 */

public class HttpClientUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    private static java.net.http.HttpClient httpClient;

    static {
        ExecutorService defaultThreadPoolExecutor = new ThreadPoolExecutor(
                20,
                1000,
                60, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(3000))
                .executor(defaultThreadPoolExecutor)
                .build();

    }

    public static <T> CompletableFuture<ApiResult<T>> get(String url, TypeReference<T> tr, Map<String, String> headerParam) {
        HttpRequest.Builder request = HttpRequest.newBuilder().GET().uri(URI.create(url)).timeout(Duration.ofMillis(1000));
        return sendAsync(request, headerParam, tr, "utf-8");
    }

    public static <T> CompletableFuture<ApiResult<T>> get(String url, TypeReference<T> tr, Map<String, String> headerParam, int timeout, String charset) {
        HttpRequest.Builder request = HttpRequest.newBuilder().GET().uri(URI.create(url)).timeout(Duration.ofMillis(timeout));
        return sendAsync(request, headerParam, tr, charset);
    }

    public static <T> CompletableFuture<ApiResult<T>> postJson(String url, String body, TypeReference<T> tr, HttpHeaders httpHeaders, int requestTimeout){
        Map<String, String> headerParam = new HashMap<>();
        for (Map.Entry<String, String> httpHeader : httpHeaders) {
            headerParam.put(httpHeader.getKey(),httpHeader.getValue());
        }
        return post(url,PostType.JSON,body,tr,headerParam,requestTimeout,"utf-8");
    }

    public static  <T> CompletableFuture<ApiResult<T>> put(String url,String body,TypeReference<T> tr, HttpHeaders httpHeaders, int requestTimeout) {
        Map<String, String> headerParam = new HashMap<>();
        headerParam.put("Content-Type","application/json");
        for (Map.Entry<String, String> httpHeader : httpHeaders) {
            headerParam.put(httpHeader.getKey(),httpHeader.getValue());
        }
        HttpRequest.Builder request = HttpRequest.newBuilder().PUT(HttpRequest.BodyPublishers.ofString(body)).uri(URI.create(url)).timeout(Duration.ofMillis(requestTimeout));
        return sendAsync(request, headerParam, tr, "utf-8");
    }

    public static <T> CompletableFuture<ApiResult<T>> post(String url, PostType postType, Object body, TypeReference<T> tr, Map<String, String> headerParam, int timeout, String charset) {
        HttpRequest.Builder request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofMillis(timeout));

        if (headerParam == null) {
            headerParam = new LinkedHashMap<>();
        }
        switch (postType) {
            case JSON:
                headerParam.put("Content-Type", "application/json");
                String bodyStr = (body instanceof String) ? (String) body : JsonUtils.toString(body);
                request.POST(HttpRequest.BodyPublishers.ofString(bodyStr));
                break;
            case X_WWW_FORM_URLENCODED:
                headerParam.put("Content-Type", "application/x-www-form-urlencoded");
                Map<String, String> map = (Map<String, String>) body;
                List<String> ps = new ArrayList<>();
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    ps.add(entry.getKey() + "=" + entry.getValue());
                }
                request.POST(HttpRequest.BodyPublishers.ofString(String.join("&", ps)));
                break;
        }
        return sendAsync(request, headerParam, tr, charset);
    }

    static <T> CompletableFuture<ApiResult<T>> sendAsync(HttpRequest.Builder request, Map<String, String> headerParam, TypeReference<T> tr,String charset) {
        if (headerParam != null && headerParam.size() > 0) {
            for (Map.Entry<String, String> entry : headerParam.entrySet()) {
                request.header(entry.getKey(), entry.getValue());
            }
        }
        ApiResult apiResult = new ApiResult();
        return httpClient.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString(Charset.forName(charset))).thenApply(response -> {
            if (response.statusCode() != 200 && response.statusCode()!=201) {
                apiResult.setCode(response.statusCode());
                apiResult.setMsg(response.body());
//                logger.error("调用原接口异常", new Exception(String.format("%s@@@%s", response.uri().getRawPath(), response.statusCode())));
                return apiResult;
            }
            apiResult.setResult(JsonUtils.toObject(response.body(), tr));
            return apiResult;
        });
    }

    public enum PostType {
        /**
         * PostBody必须为String，或者可被序列化为JSON的对象
         */
        JSON,

        /**
         * PostBody必须为Map
         */
        X_WWW_FORM_URLENCODED
    }
}
