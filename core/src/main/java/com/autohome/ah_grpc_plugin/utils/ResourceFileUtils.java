package com.autohome.ah_grpc_plugin.utils;

import java.io.IOException;
import java.io.InputStream;

public class ResourceFileUtils {
    public static  byte[] read(String path){
        InputStream in = ResourceFileUtils.class.getClassLoader().getResourceAsStream(path);
        try {
            return in.readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }
}
