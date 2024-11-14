package com.autohome.ah_grpc_plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;

import java.io.IOException;

@Service(Service.Level.PROJECT)
public final class ConfigProjectFile {

    Project project;

    public static ConfigProjectFile getInstance(Project project){
        ConfigProjectFile file = project.getService(ConfigProjectFile.class);
        if(file.project == null){
            file.project = project;
        }
        return file;
    }

    public void open(String path){
        LightVirtualFile vFile = getOrCreateFile(path);
        vFile.setWritable(false);
        FileEditorManager.getInstance(project).openFile(vFile, true, true);
    }



    public LightVirtualFile getOrCreateFile(String path) {
        for (FileEditor editor : FileEditorManager.getInstance(project).getAllEditors()) {
            VirtualFile file = editor.getFile();
            if ((file instanceof LightVirtualFile) && (file.getName().equals(path))) {
                return (LightVirtualFile) editor.getFile();
            }
        }

        String content = GitlabApiService.getContent(project, path);
        LightVirtualFile file = new LightVirtualFile(path+".config");
        try {
            file.setBinaryContent(content.getBytes("utf-8"));
        } catch (IOException ex) {
            //throw new RuntimeException(ex);
        }
        return file;
    }

}
