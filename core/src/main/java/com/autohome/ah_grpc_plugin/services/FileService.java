package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.enums.ResultCode;
import com.autohome.ah_grpc_plugin.models.MethodResult;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.protobuf.lang.psi.impl.PbImportStatementImpl;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 *  虚拟文件的处理
 */
public class FileService {

    public static boolean download(Project project, String path){
        return download(project,path,true);
    }

    public static boolean download(Project project, String path,Boolean open) {
        String content = GitlabApiService.getContent(project, path);
        if(StringUtils.isBlank(content))
            return false;
        content = content.replaceAll("\r\n","\n");
        String filePath = path.lastIndexOf("/") < 0 ? "" : path.substring(0, path.lastIndexOf("/"));
        String fileName = path.substring(path.lastIndexOf("/")+1);
        String pathName = filePath +"/"+fileName;

        CreateProtoService.create(project,filePath,fileName,content,"",(x)->{

            ProtoPsi psi = ProtoPsi.getInstance(project, pathName);
            if(psi==null)
                return;
            NotifyService.info(project,"下载成功:"+path);
            if(open) {
                open(project,pathName);
            }

            if(psi.getImports().size()==0)
                return;

            //下载导入的文件
            for (PbImportStatementImpl ip : psi.getImports()) {
                String ippath = ip.getImportName().getStringValue().getValue();
                if(FileService.exists(project,ippath))
                    continue;
                download(project, ippath,false);
            }

        });
        return true;
    }

    public static MethodResult open(Project project, String filePath){
        File fp = new File(Config.getInstance(project).protoBasePath()+filePath);
        if(!fp.exists()){
            return MethodResult.fail(ResultCode.FILE_NOT_EXISTS);
        }
        open(project, fp);
        return MethodResult.success();
    }


    public static boolean exists(Project project, String filePath){
        File file = new File(Config.getInstance(project).protoBasePath()+filePath);
        if(file==null)
            return false;
        if(!file.exists())
            return false;
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        if(vf==null)
            return false;
        return vf.exists();
    }


    public static void delete(Project project, String filePath){
        File file = new File(Config.getInstance(project).protoBasePath()+filePath);
        if(!file.exists()){
            return;
        }
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        if(vf==null)
            return;
        try {
            vf.delete(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void open(Project project, File file){
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        FileEditorManager.getInstance(project).openFile(vf,true,true);
    }

    public static VirtualFile[] getAllFiles(Project project,String path){
        File fp = new File(Config.getInstance(project).protoBasePath()+path);
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(fp);
        if(vf==null)
            return new VirtualFile[0];
        return vf.getChildren();
    }


    public static VirtualFile protoBaseFile(Project project){
        File fp = new File(Config.getInstance(project).protoBasePath()+"/autohome/rpc");
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(fp);
        return vf;
    }



    public static String getFileContent(Project project, String filePath){
        File fp = new File(Config.getInstance(project).protoBasePath()+filePath);
        if(!fp.exists())
            return "";

        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(fp);
        if(vf==null)
            return "";

        try {
            return new String(vf.contentsToByteArray(), StandardCharsets.UTF_8).replace("\r\n","\n");
        } catch (IOException e) {
            return null;
        }
    }



    public static void download(String urlPath , String targetDirectory,String fileName) throws Exception {
        File filePath = new File(targetDirectory);
        if(!filePath.exists()){
            filePath.mkdirs();
        }

        File file = new File(targetDirectory.concat(File.separator).concat(fileName));
        if(file.exists()){
            file.delete();
        }


        URL url = new URL(urlPath);
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        http.setConnectTimeout(3000);
        http.setReadTimeout(1000*60*60);
        InputStream inputStream = http.getInputStream();
        byte[] buff = new byte[1024*10];
        OutputStream out = new FileOutputStream(new File(targetDirectory,fileName));
        int len ;
        while((len = inputStream.read(buff)) != -1) {
            out.write(buff, 0, len);
            out.flush();
        }
        out.close();
        inputStream.close();
        http.disconnect();
    }


    public static String readContentFromUrl(String path){
        try {
            URL url = new URL(path);
            InputStream is = url.openStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            StringBuffer sb = new StringBuffer("");
            int len = 0;
            byte[] temp = new byte[1024];
            while ((len = bis.read(temp)) != -1) {
                sb.append(new String(temp, 0, len));
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



}
