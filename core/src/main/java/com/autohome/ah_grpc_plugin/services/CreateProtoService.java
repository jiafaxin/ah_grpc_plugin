package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.enums.ResultCode;
import com.autohome.ah_grpc_plugin.models.MethodResult;
import com.autohome.ah_grpc_plugin.providers.services.ImplProtoService;
import com.autohome.ah_grpc_plugin.utils.PathUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.protobuf.lang.PbFileType;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class CreateProtoService {

    JBPopup popup;

    JBTextField fileNameTextFiled;

    JBList list;

    Border fileNameBorder = new EmptyBorder(2,2,2,2);

    public MethodResult create(Project project, String path, Consumer<String> callback) {

        JPanel box = new JPanel(new VerticalFlowLayout(0, 0));
        box.setBackground(new Color(69, 73, 74));

        JBPanel filenamePanel = new JBPanel(new VerticalFlowLayout(0, 0));
        fileNameTextFiled = new JBTextField();
        fileNameTextFiled.setBackground(null);
        fileNameTextFiled.setBorder(fileNameBorder);
        filenamePanel.add(fileNameTextFiled);
        filenamePanel.setBorder(new MatteBorder(1, 0, 1, 0, new Color(100, 100, 100)));
        filenamePanel.setBackground(new Color(60, 63, 65));

        box.add(filenamePanel);

        JBLabel label = new JBLabel("以字母开头，包含小写字母，数字和下划线");
        label.setBorder(new EmptyBorder(5, 5, 5, 5));
        label.setForeground(Color.GRAY);
        box.add(label);

        list = new JBList();
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("创建标准契约");
        model.addElement("创建空契约");
        list.setModel(model);
        list.setSelectedIndex(0);
        list.setBackground(new Color(60, 63, 65));
        list.setCellRenderer((list1, value, index, isSelected, cellHasFocus) -> {
            JBPanel panel = new JBPanel(new VerticalFlowLayout());
            JBLabel label1 = new JBLabel((String) value);
            panel.add(label1);
            if (isSelected) {
                panel.setBackground(list.getSelectionBackground());
                panel.setForeground(list.getSelectionForeground());
            } else {
                panel.setBackground(list.getBackground());
                panel.setForeground(list.getForeground());
            }
            return panel;
        });

        KeyAdapter adapter = getSelectKeyAdapter();
        fileNameTextFiled.addKeyListener(adapter);

        box.add(list);

        popup = JBPopupFactory.getInstance().createComponentPopupBuilder(box, fileNameTextFiled).setTitle("创建契约文件(.proto)")
                .setFocusable(true)
                .setMovable(true)
                .setProject(project)
                .setRequestFocus(true).setMinSize(new Dimension(360, 0))
                .createPopup();

        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                JBPopupListener.super.onClosed(event);
                if (!isMatch()) {
                    return;
                }
                JBPopupListener.super.onClosed(event);
                String fileName = fileNameTextFiled.getText();
                fileName = fileName.concat(".proto");
                String finalFileName = fileName;
                boolean isEmptyProto = list.getSelectedIndex()==1;
                create(project, path, finalFileName,isEmptyProto, (x) -> {
                    callback.accept(finalFileName);
                });
            }
        });

        popup.showCenteredInCurrentWindow(project);
        return MethodResult.success();
    }

    KeyAdapter getSelectKeyAdapter() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);

                int selectedIndex = list.getSelectedIndex();
                int size = list.getItemsCount();

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        if (selectedIndex == 0)
                            selectedIndex = size - 1;
                        else
                            selectedIndex--;
                        break;
                    case KeyEvent.VK_DOWN:
                        if (selectedIndex == size - 1)
                            selectedIndex = 0;
                        else
                            selectedIndex++;
                        break;

                    case KeyEvent.VK_ENTER:
                        if (isMatch()) {
                            popup.cancel();
                        }
                        return;
                }
                list.setSelectedIndex(selectedIndex);
            }
        };
    }




    MatteBorder errorBorder = new MatteBorder(2,2,2,2,new Color(114,82,82));

    public boolean isMatch(){
        String name = fileNameTextFiled.getText();
        if(StringUtils.isBlank(name)) {
            fileNameTextFiled.setBorder(errorBorder);
            return false;
        }
        String pattern = "[a-z][a-z0-9_]{0,}[a-z0-9]{0,}";
        boolean isMatch = Pattern.matches(pattern, name);
        if(!isMatch){
            fileNameTextFiled.setBorder(errorBorder);
            return false;
        }
        fileNameTextFiled.setBorder(new EmptyBorder(2,2,2,2));
        return true;
    }


    public static MethodResult create(Project project,String path, String fileName, Consumer<String> callback){
        return create(project, path,fileName,false,callback);
    }

    public static MethodResult create(Project project, String path, String fileName,boolean emptyProto, Consumer<String> callback){
        if(!GitlabApiService.loginIfNot(project)){
            return MethodResult.fail(ResultCode.USER_NOT_LOGIN);
        }
        String content = getTemplate(project,path,fileName,emptyProto);
        return create(project,path,fileName,content,"创建失败：文件已存在！",callback);
    }

    public static MethodResult create(Project project, String path, String fileName, String content, String alertIfExists, Consumer<String> callback){
        String baseFolder = Config.getInstance(project).getProtoBaseFolder();
        if(baseFolder==null){
            NotifyService.error(project,"Project 不支持 gRPC");
            return MethodResult.fail(ResultCode.GRPC_NOT_EXISTS);
        }
        ApplicationManager.getApplication().runWriteAction(()->{
            PsiDirectory directory = PsiService.createAndGetDirectory(project,baseFolder+path);
            PsiFile file = directory.findFile(fileName);
            if(file!=null){
                if(StringUtils.isNotBlank(alertIfExists)) {
                    NotifyService.error(project,alertIfExists);
                }
                return;
            }
            file = PsiFileFactory.getInstance(project).createFileFromText(fileName, PbFileType.INSTANCE,content);

            directory.add(file);
            file = directory.findFile(fileName);
            VirtualFile vf = file.getVirtualFile();
            FileChangeLog.getInstance(project).init( vf);
            vf.refresh(true,false);
            if(vf.getFileType() instanceof PbFileType) {
                FileEditorManager.getInstance(project).openFile(vf,true,true);
                ImplProtoService.getInstance(project).initProto(file.getVirtualFile());
                String np = PathUtils.getProtoPath(project, vf.getPath());
                if(np==null)
                    return;
                GitLocalService.getInstance(project).create(np);
                downloadApiProto(project);
            }
            callback.accept(path);
        });
        return MethodResult.success();
    }

    public static String getTemplate(Project project, String path,String name,boolean emptyProto) {
        return ProtoContentService.getTemplate(project, path,name,emptyProto);
    }

    public static void downloadApiProto(Project project){
        String path = "autohome/api/annotations.proto";
        if(FileService.exists(project,path))
            return;
        FileService.download(project,path);
    }
}
