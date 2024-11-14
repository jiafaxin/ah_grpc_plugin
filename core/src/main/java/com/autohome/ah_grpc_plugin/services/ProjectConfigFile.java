package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.models.PermissionDto;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.TextEditorWithPreview;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;

import java.io.IOException;

import static com.intellij.openapi.fileEditor.TextEditorWithPreview.DEFAULT_LAYOUT_FOR_FILE;

@Service(Service.Level.PROJECT)
public final class ProjectConfigFile {

    Project project;

    public static ProjectConfigFile getInstance(Project project){
        ProjectConfigFile file = project.getService(ProjectConfigFile.class);
        if(file.project == null){
            file.project = project;
        }
        return file;
    }

    public void open(String path){
        LightVirtualFile vFile = getOrCreateFile(path);
        vFile.setWritable(false);
        ApplicationManager.getApplication().runWriteAction(()-> {
            PermissionDto permission = AuthService.getInstance(project).hasPermission(path, GitlabApiService.getCurrentUser());
            if (permission.isHasPermission()) {
                vFile.setWritable(true);
            }
        });
        vFile.putUserData(DEFAULT_LAYOUT_FOR_FILE, TextEditorWithPreview.Layout.SHOW_PREVIEW);
        FileEditorManager.getInstance(project).openFile(vFile, true);
    }

    public LightVirtualFile getOrCreateFile(String path) {
        for (FileEditor editor : FileEditorManager.getInstance(project).getAllEditors()) {
            VirtualFile file = editor.getFile();
            if ((file instanceof LightVirtualFile) && (file.getName().equals(path))) {
                return (LightVirtualFile) editor.getFile();
            }
        }
        String realPath = path+"/readme.md";
        String content = GitlabApiService.getContent(project, realPath);
        LightVirtualFile file = new LightVirtualFile(realPath);
        try {
            file.setBinaryContent(content.getBytes("utf-8"));
        } catch (IOException ex) {
            //throw new RuntimeException(ex);
        }
        return file;
    }

}
