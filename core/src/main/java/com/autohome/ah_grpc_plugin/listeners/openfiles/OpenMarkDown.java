package com.autohome.ah_grpc_plugin.listeners.openfiles;

import com.autohome.ah_grpc_plugin.models.PermissionDto;
import com.autohome.ah_grpc_plugin.models.ProtoPath;
import com.autohome.ah_grpc_plugin.services.AuthService;
import com.autohome.ah_grpc_plugin.services.GitService;
import com.autohome.ah_grpc_plugin.services.GitlabApiService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;

public class OpenMarkDown {

    public OpenMarkDown(Project project, @NotNull FileEditorManager source, @NotNull VirtualFile file){
        this.project = project;
        this.source = source;
        this.file = file;
    }
    FileEditorManager source;

    VirtualFile file;

    Project project;

    public void close(){
        ProtoPath path = ProtoPath.newInstance(file.getPath().replace("/readme.md",""));
        if(path==null||!path.isProject()){
            return;
        }
        String filePath =  path.getProjectPath()+"/readme.md";
        saveOrUpdate(filePath,FileDocumentManager.getInstance().getDocument(file));
    }

    public void open() {
        ProtoPath path = ProtoPath.newInstance(file.getPath().replace("/readme.md",""));
        if(path==null||!path.isProject()){
            return;
        }

        FileEditor[] fes = source.getEditors(file);
        if (fes == null || fes.length == 0)
            return;

        Document document = FileDocumentManager.getInstance().getDocument(file);
//        document.setReadOnly(true);

//        ApplicationManager.getApplication().runWriteAction(()-> {
//            PermissionDto permission = AuthService.getInstance(project).hasPermission(path.getProjectPath(), GitlabApiService.getCurrentUser());
//            if (permission.isHasPermission()) {
//                document.setReadOnly(false);
//            }
//        });

        FileDocumentManager.getInstance().getDocument(file).addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                DocumentListener.super.documentChanged(event);
                String filePath =  path.getProjectPath()+"/readme.md";
                saveOrUpdate(filePath,event.getDocument());
            }
        });
    }


    public void saveOrUpdate(String filePath , Document document){
        String content = document.getText();
        GitlabApiService.updateFile(project,filePath,content,"编辑应用说明").thenAcceptAsync(x->{
            if(x!=null && x.getCode()==400 && x.getMsg().equals("{\"message\":\"A file with this name doesn't exist\"}")){
                GitlabApiService.createFile(project,filePath,content,"没有文件，新建");
            }
        });
    }
}
