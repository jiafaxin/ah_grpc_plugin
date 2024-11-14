package com.autohome.ah_grpc_plugin.utils;

import com.autohome.ah_grpc_plugin.services.FileService;
import com.autohome.ah_grpc_plugin.services.NotifyService;
import com.intellij.execution.ui.ConsoleViewContentType;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

public class FileDownloadUtils {

    /**
     * 检查文件内容是否一致
     * @param localPath
     * @param path
     * @return
     */
    public static boolean check(String localPath,String path){
        File checkFile = new File(localPath);
        if (checkFile.exists()) {
            String lastCheckSum = FileService.readContentFromUrl(path);
            if (StringUtils.isBlank(lastCheckSum)) {
                //如果远程没有文件，则默认当前就是最新的
                return true;
            }

            String checksum = "";
            try (FileInputStream inputStream = new FileInputStream(checkFile)) {
                checksum = IOUtils.toString(inputStream, "utf-8");
            } catch (Exception e) {

            }

            if (checksum.equals(lastCheckSum)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSameFiles(File file1, File file2) {
        try {
            InputStream fileStream1 = new FileInputStream(file1);
            InputStream fileStream2 = new FileInputStream(file2);
            String firstFileMd5 = DigestUtils.md5Hex(new byte[fileStream1.available()]);
            String secondFileMd5 = DigestUtils.md5Hex(new byte[fileStream2.available()]);
            if (firstFileMd5.equals(secondFileMd5)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
