package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.models.MethodResult;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;

import java.io.File;
import java.io.IOException;

/**
 * 虚拟文件的处理
 */
public class VirtualFileService {

    public static void open(Project project,String path) {
        MethodResult methodResult = FileService.open(project, path);
        if (methodResult.isSuccess())
            return;

        LightVirtualFile vFile = getOrCreateFile(project,path);
        vFile.setWritable(false);
        FileEditorManager.getInstance(project).openFile(vFile, true, true);
    }


    public static LightVirtualFile getOrCreateFile(Project project, String path) {
        for (FileEditor editor : FileEditorManager.getInstance(project).getAllEditors()) {
            VirtualFile file = editor.getFile();
            if ((file instanceof LightVirtualFile) && (file.getName().equals(path))) {
                return (LightVirtualFile) editor.getFile();
            }
        }

        String content = GitlabApiService.getContent(project, path);
        LightVirtualFile file = new LightVirtualFile(path);
        try {
            file.setBinaryContent(content.getBytes("utf-8"));
        } catch (IOException ex) {
            //throw new RuntimeException(ex);
        }
        return file;
    }


    public static VirtualFile get(String path){
        File file = new File(path);
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        return virtualFile;
    }


}
