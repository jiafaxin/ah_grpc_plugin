package com.autohome.ah_grpc_plugin.utils;

import com.autohome.ah_grpc_plugin.Config;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.util.SystemInfo;
import com.openhtmltopdf.swing.NaiveUserAgent;

import javax.swing.table.TableRowSorter;

public final class OsUtil {
    public static boolean isWindows() {
        return SystemInfo.isWindows;
    }



    public static String baseExePath(){
        if(isWindows()){
            return "powershell";
        }
        return "/bin/bash";
    }

}
