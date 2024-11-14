package com.autohome.ah_grpc_plugin.utils;

import com.squareup.okhttp.*;

import java.io.IOException;

public class HttpClient {

    private final static OkHttpClient client = new OkHttpClient();

    public static Response get(String url) {
        MediaType mediaType = MediaType.parse("application/json");
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("PRIVATE-TOKEN","gdLmW8s7LJGjy9j5HgqS")
                .build();
        return execute(request);
    }

    static Response execute(Request request){
        try {
            return client.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
