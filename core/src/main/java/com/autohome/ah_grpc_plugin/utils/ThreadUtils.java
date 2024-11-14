package com.autohome.ah_grpc_plugin.utils;

public class ThreadUtils {
    public static void sleep(int ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
