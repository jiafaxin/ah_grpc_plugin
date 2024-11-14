package com.autohome.ah_grpc_plugin.utils;

import java.io.File;

public class LocalPathUtil {
    public static String localPath() {
        String base;
        if(OsUtil.isWindows()){
            base = System.getenv("LOCALAPPDATA");
        }else{
            base = System.getenv("HOME");
        }
        return base.concat(File.separator).concat("autohome").concat(File.separator).concat("grpc").concat(File.separator);

    }
}
