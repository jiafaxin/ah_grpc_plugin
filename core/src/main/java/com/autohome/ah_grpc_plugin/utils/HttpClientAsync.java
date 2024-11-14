//package com.autohome.ah_grpc_plugin.utils;
//
//import com.autohome.ah_grpc_plugin.models.ApiResult;
//import com.autohome.ah_grpc_plugin.services.NotifyService;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.intellij.openapi.application.ApplicationManager;
//import com.intellij.openapi.components.Service;
//import com.intellij.openapi.project.Project;
//import io.netty.handler.codec.http.HttpHeaders;
//import io.netty.handler.codec.http.cookie.Cookie;
//import org.asynchttpclient.*;
//
//import java.net.http.HttpClient;
//import java.nio.charset.Charset;
//import java.util.Collection;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
//import static org.asynchttpclient.Dsl.asyncHttpClient;
//
///**
// * crated by shicuining 2021/1/8
// */
//@Service
//public final class HttpClientAsync {
//
//    Project project;
//
//    public static HttpClientAsync getInstance(Project project) {
//        HttpClientAsync httpClientAsync = ApplicationManager.getApplication().getService(HttpClientAsync.class);
//        if (httpClientAsync.project == null) {
//            httpClientAsync.project = project;
//        }
//
//        return httpClientAsync;
//    }
//
//    private AsyncHttpClient client;
//
//    public AsyncHttpClient getClient() {
//        if(client==null) {
//            DefaultAsyncHttpClientConfig.Builder clientBuilder = Dsl.config().setConnectTimeout(60000);
//            client = asyncHttpClient(clientBuilder);
//        }
//        return client;
//    }
//
//    public <T> CompletableFuture<ApiResult<T>> head(String url, List<Param> queryParams, TypeReference<T> tr, HttpHeaders httpHeaders, Collection<Cookie> cookies, int requestTimeout, String charset) {
//        BoundRequestBuilder request = getClient().prepareHead(url).setRequestTimeout(requestTimeout);
//
//        if(queryParams!=null && queryParams.size()>0) {
//            request.setQueryParams(queryParams);
//        }
//
//        return execute(request,tr,charset,httpHeaders,cookies);
//    }
//
////    public <T> CompletableFuture<ApiResult<T>> get(String url, List<Param> queryParams, TypeReference<T> tr, HttpHeaders httpHeaders, Collection<Cookie> cookies, int requestTimeout, String charset) {
////        BoundRequestBuilder request = getClient().prepareGet(url).setRequestTimeout(requestTimeout);
////
////        if(queryParams!=null && queryParams.size()>0) {
////            request.setQueryParams(queryParams);
////        }
////
////        return execute(request,tr,charset,httpHeaders,cookies);
////    }
//
//
//    public <T> CompletableFuture<ApiResult<T>> postJson(String url,String body,TypeReference<T> tr, HttpHeaders httpHeaders, int requestTimeout) {
//        BoundRequestBuilder request = getClient().preparePost(url).setRequestTimeout(requestTimeout);
//        httpHeaders.add("Content-Type","application/json");
//        request.setBody(body);
//        return execute(request,tr,"utf-8",httpHeaders,null);
//    }
//
//    public <T> CompletableFuture<ApiResult<T>> put(String url,String body,TypeReference<T> tr, HttpHeaders httpHeaders, int requestTimeout) {
//        BoundRequestBuilder request = getClient().preparePut(url).setRequestTimeout(requestTimeout);
//        httpHeaders.add("Content-Type","application/json");
//        request.setBody(body);
//        return execute(request,tr,"utf-8",httpHeaders,null);
//    }
//
//    <T> CompletableFuture<ApiResult<T>> execute(BoundRequestBuilder request,TypeReference<T> tr, String charset,HttpHeaders httpHeaders, Collection<Cookie> cookies){
//        request.setReadTimeout(5000);
//        if(httpHeaders!=null && httpHeaders.size()>0) {
//            request.setHeaders(httpHeaders);
//        }
//        if(cookies!=null && cookies.size()>0) {
//            request.setCookies(cookies);
//        }
//        return execute(request,tr,charset);
//    }
//
//   <T> CompletableFuture<ApiResult<T>> execute(BoundRequestBuilder request,TypeReference<T> tr, String charset){
//       ApiResult apiResult = new ApiResult();
//        return  request.execute(new AsyncCompletionHandler<ApiResult<T>>() {
//            @Override
//            public ApiResult<T> onCompleted(Response response) {
//                if(response.getStatusCode()>=300) {
//                    apiResult.setCode(response.getStatusCode());
//                    apiResult.setMsg(response.getResponseBody(Charset.forName(charset)));
//                }
//                if(response.getStatusCode()>=500){
//                    NotifyService.error(project,"访问接口出错:["+response.getStatusCode()+"] " + response.getResponseBody(Charset.forName(charset)));
//                }
//                try {
//                    T result;
//                    if(tr.getType().getTypeName().equals(String.class.getTypeName())){
//                        result = (T)response.getResponseBody(Charset.forName(charset));
//                    }else {
//                        //只有utf-8的支持流式反序列化
//                        if (charset.equalsIgnoreCase("utf-8")) {
//                            result = JsonUtils.toObject(response.getResponseBodyAsStream(), tr);
//                        }else {
//                            result = JsonUtils.toObject(response.getResponseBody(Charset.forName(charset)), tr);
//                        }
//                    }
//                    apiResult.setResult(result);
//                }catch (Exception e){
//
//                }
//
//                return apiResult;
//            }
//        }).toCompletableFuture();
//   }
//
//}
