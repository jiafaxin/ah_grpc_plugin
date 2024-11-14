package com.autohome.ah_grpc_plugin.listeners;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.dialogs.Confim;
import com.autohome.ah_grpc_plugin.enums.GitLocalInfoType;
import com.autohome.ah_grpc_plugin.listeners.openfiles.OpenMarkDown;
import com.autohome.ah_grpc_plugin.listeners.openfiles.OpenProto;
import com.autohome.ah_grpc_plugin.models.PermissionDto;
import com.autohome.ah_grpc_plugin.models.ProtoPath;
import com.autohome.ah_grpc_plugin.providers.services.ImplProtoService;
import com.autohome.ah_grpc_plugin.services.*;
import com.autohome.ah_grpc_plugin.ui.MainToolwindow;
import com.autohome.ah_grpc_plugin.utils.Backgrounds;
import com.autohome.ah_grpc_plugin.utils.BorderColors;
import com.autohome.ah_grpc_plugin.utils.PathUtils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.protobuf.lang.PbFileType;
import com.intellij.protobuf.lang.psi.impl.PbImportStatementImpl;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 监听打开Proto文件
 */
public class OpenProtoListener implements FileEditorManagerListener {

    private final Project project;

    protected OpenProtoListener(Project project)
    {
        this.project = project;
    }


    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if(!GrpcConfigService.getInstance(project).getSupportGrpc()){
            return;
        }
        if ((file.getFileType() instanceof PbFileType)) {
            new OpenProto(project,source,file).open();
            return;
        }
        if(file.getPath().endsWith(".md")){
            new OpenMarkDown(project,source,file).open();
        }
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile sourceFile) {
        if(sourceFile.getPath().endsWith(".md")){
            new OpenMarkDown(project,source,sourceFile).close();
        }
//        if(!GrpcConfigService.getInstance(project).getSupportGrpc()){
//            return;
//        }
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
//        if(!GrpcConfigService.getInstance(project).getSupportGrpc()){
//            return;
//        }
    }
}
