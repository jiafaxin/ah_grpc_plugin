package com.autohome.ah_grpc_plugin.listeners.openfiles;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.dialogs.Confim;
import com.autohome.ah_grpc_plugin.enums.GitLocalInfoType;
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
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.Arrays;

public class OpenProto {

    public OpenProto(Project project, @NotNull FileEditorManager source, @NotNull VirtualFile file){
        this.project = project;
        this.source = source;
        this.file = file;
    }
    FileEditorManager source;
    FileEditor fileEditor;

    JComponent topComponent;

    Document document;

    VirtualFile file;

    Project project;

    public void open(){
        ImplProtoService.getInstance(project).initProto(file);
        FileEditor[] fes = source.getEditors(file);
        if (fes == null || fes.length == 0)
            return;
        fileEditor = fes[0];


        document = FileDocumentManager.getInstance().getDocument(file);
        document.setReadOnly(true);

        setTopComponent();

        FileDocumentManager.getInstance().getDocument(file).addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                DocumentListener.super.documentChanged(event);
                ProtoPath path = ProtoPath.newInstance(file.getPath());
                if (path == null)
                    return;

                if(FileChangeLog.getInstance(project).hasChangeDocument(file)){
                    GitLocalService.getInstance(project).update( path.realPath());
                    MainToolwindow.getInstance(project).itemContentUpdate(path.realPath());
                }else{
                    GitLocalService.getInstance(project).delete( path.realPath(), GitLocalInfoType.update);
                    MainToolwindow.getInstance(project).itemContentNotUpdate(path.realPath());
                }
            }
        });
    }

    public void setTopComponent(){
        ApplicationManager.getApplication().runWriteAction(()->{

            String rdlPath = GrpcConfigService.getInstance(project).getIdlPath();
            ProtoPath protoPath = ProtoPath.newInstance(file.getPath());
            if(protoPath==null){
                NotifyService.error(project, file.getPath()+" 不是一个proto文件");
                document.setReadOnly(false);
                return;
            }
            boolean readOnly = true;
            if(rdlPath==null){
                document.setReadOnly(false);
                readOnly = false;
            }else if(rdlPath.equals(protoPath.getProjectPath())){
                document.setReadOnly(false);
                readOnly = false;
            }

            if(topComponent != null){
                FileEditorManager.getInstance(project).removeTopComponent(fileEditor,topComponent);
            }
            topComponent = getEditorTopComponent(file,readOnly);
            FileEditorManager.getInstance(project).addTopComponent(fileEditor, topComponent);
        });
    }

    JComponent getEditorTopComponent(VirtualFile file,boolean readOnly) {

        JPanel box = new JPanel(new BorderLayout());
        if (file instanceof LightVirtualFile) {
            JButton pullBtn = new JButton("下载当前文件到本地", AllIcons.Actions.TraceInto);
            pullBtn.addActionListener((e) -> {
                FileService.download(project, file.getPath().substring(1));
                FileEditorManager.getInstance(project).closeFile(file);
                ProtoPath path = ProtoPath.newInstance(file.getPath());
                if(path==null)
                    return;
                MainToolwindow.getInstance(project).refreshStatus();
            });
            box.add(pullBtn,BorderLayout.EAST);
            JBLabel label = new JBLabel("当前文件是远程文件，无法使用或编辑；如需使用或编辑，请先下载！");
            box.add(label,BorderLayout.CENTER);
        } else {
            JBPanel right = new JBPanel(new FlowLayout(FlowLayout.RIGHT));
            if(readOnly==false) {
                JButton bufBtn = new JButton("代码检查", AllIcons.General.InspectionsTypos);
                bufBtn.addActionListener((e) -> {
                    CommandService.buf(project);
                });
                right.add(bufBtn);
            }

            if(Config.getInstance(project).isGo()) {
                ProtoPath path = ProtoPath.newInstance(file.getPath());
                if (path != null) {
                    JButton genBtn = new JButton("生成Stub");
                    genBtn.addActionListener((e) -> {
                        FileDocumentManager.getInstance().saveAllDocuments();
                        GenProtoService.getInstance(project).compile(Arrays.asList(path.realPath()));
                    });
                    right.add(genBtn);
                }
            }
            if(readOnly) {
                right.add(getPullBtn(project, file));
            }
            box.add(right,BorderLayout.WEST);
        }

        JPanel body = new JPanel(new VerticalFlowLayout(0,0));
        body.add(box);

        //没有登录
        if(readOnly) {
            JBPanel warning = new JBPanel(new FlowLayout(FlowLayout.LEFT));
            warning.setBackground(Backgrounds.error);
            warning.setBorder(new MatteBorder(1,0,0,0, BorderColors.base));
            JBLabel label = new JBLabel();
            label.setText("当前proto文件不属于当前应用，禁止修改！" );
            warning.add(label);
            body.add(warning);
        }
        return body;
    }

    /**
     * 覆盖本地文件
     * @param project
     * @param file
     * @return
     */
    JButton getPullBtn(Project project, VirtualFile file){

        JButton pullBtn = new JButton("拉取", AllIcons.Actions.TraceInto);
        pullBtn.setEnabled(false);
        String protoPath = PathUtils.getProtoPath(project, file.getPath());
        GitlabApiService.getFileAsync(project, protoPath).thenAccept((fileDetail -> {
            if (fileDetail != null) {
                pullBtn.setEnabled(true);
            }
        }));

        pullBtn.addActionListener((e) -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                ProtoFileService.reload(project, protoPath);
            });
        });

        return pullBtn;
    }

}
