package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.models.FileChangeInfo;
import com.autohome.ah_grpc_plugin.models.ProtoPath;
import com.autohome.ah_grpc_plugin.ui.MainToolwindow;
import com.autohome.ah_grpc_plugin.utils.JsonUtils;
import com.intellij.json.JsonFileType;
import com.intellij.json.JsonLanguage;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonValue;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.protobuf.lang.PbFileType;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service(Service.Level.PROJECT)
public final class FileChangeLog {

    Project project;

    public static FileChangeLog getInstance(Project project) {
        FileChangeLog fileChangeLog = project.getService(FileChangeLog.class);
        if(fileChangeLog.project == null){
            fileChangeLog.project = project;
            fileChangeLog.scanAll();
        }
        return fileChangeLog;
    }

    JsonFile jsonFile;

    /**
     * 创建文件的时候，初始化
     * @param virtualFile
     */
    public void init(VirtualFile virtualFile){
        ProtoPath path = ProtoPath.newInstance(virtualFile.getPath());
        if(path == null)
            return;
        write( path.realPath(),getSha256(FileDocumentManager.getInstance().getDocument(virtualFile)));
    }


    public boolean hasChange( VirtualFile virtualFile) {
        if(virtualFile == null)
            return false;

        ProtoPath path = ProtoPath.newInstance(virtualFile.getPath());
        if (path == null)
            return false;

        String sha256 = getSha256(virtualFile);
        return hasChange(path.realPath(),sha256);
    }


    public boolean hasChangeDocument(VirtualFile virtualFile) {
        if (virtualFile == null)
            return false;

        ProtoPath path = ProtoPath.newInstance(virtualFile.getPath());
        if (path == null)
            return false;

        byte[] data = FileDocumentManager.getInstance().getDocument(virtualFile).getText().getBytes(StandardCharsets.UTF_8);

        String sha256 = getSha256(data);
        return hasChange(path.realPath(),sha256);
    }

    public boolean hasChange(String path,String sha256){
        String fileSha256 = getSha256FromFile( path);
        if (fileSha256 == null) {
            write(path, sha256);
            return false;
        }
        return !fileSha256.equals(sha256);
    }

    public String getSha256FromFile(String path) {
        JsonFile file = getFile(project);
        return ApplicationManager.getApplication().runReadAction((Computable<String>)()->{
            for (JsonValue jsonValue : file.getAllTopLevelValues()) {
                FileChangeInfo info = JsonUtils.toObject(jsonValue.getText(), FileChangeInfo.class);
                if (info == null || info.getPath() == null)
                    continue;
                if (info.getPath().equals(path))
                    return info.getSha256();
            }
            return null;
        });
    }


    /**
     * 初始化filechangelog时
     * 提交之后，移除一些文件，然后重新扫描
     */
    public void scanAll(){
        String path = Config.getInstance(project).protoBasePath();
        if(path==null)
            return;
        File base = new File(Config.getInstance(project).protoBasePath());
        if(!base.exists()){
            return;
        }
        scan(LocalFileSystem.getInstance().findFileByIoFile(base));
    }

    void scan(VirtualFile directory){
        for (VirtualFile child : directory.getChildren()) {
            if(child.isDirectory()){
                scan(child);
            }else if(child.getFileType() instanceof PbFileType){
                initFile( child);
            }
        }
    }

    void initFile(VirtualFile file){
        ProtoPath path = ProtoPath.newInstance(file.getPath());
        if(path == null)
            return;
        FileChangeInfo info = new FileChangeInfo();
        info.setPath(path.realPath());
        info.setSha256(getSha256(file));
        write(info);
    }

    void write( String path,String sha256){
        write(new FileChangeInfo() {{
            setPath(path);
            setSha256(sha256);
        }});
    }

    synchronized void write( FileChangeInfo info) {
        if(info==null||info.getPath()==null)
            return;
        String content = JsonUtils.toString(info);
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                JsonFile gitFile = getFile(project);
                for (JsonValue item : gitFile.getAllTopLevelValues()) {
                    FileChangeInfo infoItem = JsonUtils.toObject(item.getText(), FileChangeInfo.class);
                    if (infoItem.getPath().equals(info.getPath())) {
                        return;
                    }
                }
                JsonFile file = (JsonFile) PsiFileFactory.getInstance(project).createFileFromText(JsonLanguage.INSTANCE, content);
                gitFile.add(file.getTopLevelValue());
            });
        });
    }

    public void removeList(List<String> paths) {
        ApplicationManager.getApplication().invokeLater(() -> {
            for (String path : paths) {
                MainToolwindow.getInstance(project).itemContentNotUpdate(path);
            }

            WriteCommandAction.runWriteCommandAction(project, () -> {
                JsonFile gitFile = getFile(project);
                List<JsonValue> removeList = new ArrayList<>();
                for (JsonValue item : gitFile.getAllTopLevelValues()) {
                    FileChangeInfo infoItem = JsonUtils.toObject(item.getText(), FileChangeInfo.class);
                    if (paths.contains(infoItem.getPath())) {
                        removeList.add(item);
                    }
                }
                for (JsonValue jsonValue : removeList) {
                    jsonValue.delete();
                }
                scanAll();
            });
        });
    }

    public JsonFile getFile(Project project){
        return getAndCreateFile();
    }

    synchronized JsonFile getAndCreateFile() {
        String fileName = "filechange.json";
        String path = project.getBasePath() + "/.idea";

        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(path + "/" + fileName));
        if (file == null) {
            return ApplicationManager.getApplication().runWriteAction((Computable<JsonFile>) () -> {
                PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, JsonFileType.INSTANCE, "");
                PsiDirectory psiDirectory = PsiService.createAndGetDirectory(project,"/.idea");
                psiDirectory.add(psiFile);
                psiFile = psiDirectory.findFile(fileName);
                deleteFromProjectGit();
                return (JsonFile) psiFile;
            });
        } else {
            return ApplicationManager.getApplication().runReadAction((Computable<JsonFile>) () -> {
                return (JsonFile) PsiManager.getInstance(project).findFile(file);
            });
        }
    }

    void deleteFromProjectGit() {
        try {
            File file = new File(project.getBasePath() + "/.idea/.gitignore");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWritter = new FileWriter(file, true);
            BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
            bufferWritter.write("\n.idea/filechange.json");
            bufferWritter.close();
        } catch (Exception e) {

        }
    }



    static String getSha256(VirtualFile virtualFile) {
        try {
            String content = new String(virtualFile.contentsToByteArray());
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            String sha256 = getSha256(data);
            return sha256;
        } catch (Exception e) {
            return "";
        }
    }

    static String getSha256(Document document) {
        try {
            byte[] data = document.getText().getBytes(StandardCharsets.UTF_8);
            String sha256 = getSha256(data);
            return sha256;
        } catch (Exception e) {
            return "";
        }
    }

    static String getSha256(byte[] data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            String result = new BigInteger(1, hash).toString(16);
            return result;
        } catch (Exception e) {
            return "";
        }
    }

}
