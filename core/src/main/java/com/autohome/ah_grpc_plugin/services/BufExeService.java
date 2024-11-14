package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.utils.LocalPathUtil;
import com.autohome.ah_grpc_plugin.utils.OsUtil;
import com.autohome.ah_grpc_plugin.utils.PathUtils;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BufExeService {

    static final String urlBase ="http://nfiles3.autohome.com.cn/cars/buf/v1110/";
    static String checksumName(){
        return fileName() + ".checksum";
    }

    public static String localPath() {
        return LocalPathUtil.localPath();
    }

    static String fileName() {
        return OsUtil.isWindows() ? "buf.exe" : "buf-mac-amd64";
    }

    public static String localFilePath(){
        return localPath().concat(fileName());
    }

    public static boolean downExe(Project project, ConsoleView consoleView) {
        String v = "?v="+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        File checkFile = new File(localPath() + checksumName());
        if (checkFile.exists()) {
            String lastCheckSum = FileService.readContentFromUrl(urlBase + checksumName()+v);
            if (StringUtils.isBlank(lastCheckSum)) {
                if(consoleView!=null) {
                    consoleView.print("[版本检查] 获取buf版本失败，请检查您的网络环境 \n", ConsoleViewContentType.LOG_ERROR_OUTPUT);
                }
                NotifyService.error(project,"获取buf版本失败，请检查您的网络环境");
                return false;
            }

            String checksum = "";
            try (FileInputStream inputStream = new FileInputStream(checkFile)) {
                checksum = IOUtils.toString(inputStream, "utf-8");
            } catch (Exception e) {
            }

            if (checksum.equals(lastCheckSum)) {
                if(consoleView!=null) {
                    consoleView.print("[版本检查] 版本检查通过 \n", ConsoleViewContentType.LOG_INFO_OUTPUT);
                }
                return true;
            }
            checkFile.delete();
        }

        if(consoleView!=null) {
            consoleView.print("[版本检查] 版本检查未通过，正在重新下载"+fileName()+" \n", ConsoleViewContentType.LOG_ERROR_OUTPUT);
        }

        File file = new File(localFilePath());
        if (file.exists()) {
            file.delete();
        }


        File filePath = new File(localPath());
        if (!filePath.exists()) {
            filePath.mkdirs();
        }

        try {
            FileService.download(urlBase + fileName()+v, localPath(), fileName());
            FileService.download(urlBase + checksumName()+v, localPath(), checksumName());
        } catch (Exception e) {
            consoleView.print("[版本检查] 下载失败：" + e.getMessage() + " \n", ConsoleViewContentType.LOG_ERROR_OUTPUT);
            return false;
        }

        if(consoleView!=null) {
            consoleView.print("[版本检查] 下载完成 \n", ConsoleViewContentType.LOG_WARNING_OUTPUT);
        }
        return true;
    }

}
