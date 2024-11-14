package com.autohome.ah_grpc_plugin.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class UrlUtils {
    public static String encode(String url){
        if(url==null)
            return url;
        try {
            return URLEncoder.encode(url,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            return url;
        }
    }
}
