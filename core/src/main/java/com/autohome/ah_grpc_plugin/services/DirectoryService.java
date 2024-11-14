package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.enums.NodeType;
import com.autohome.ah_grpc_plugin.enums.ResultCode;
import com.autohome.ah_grpc_plugin.models.MethodResult;
import com.autohome.ah_grpc_plugin.utils.PathUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class DirectoryService  {

    JBPopup popup;

    JBTextField nameTextFiled;

    String pattern = "";

    JPanel box;

    JBPanel fieldBox;

    Project project;


    public MethodResult create(Project project, String path, Consumer<String> callback){
        this.project = project;
        fieldBox = new JBPanel(new VerticalFlowLayout(0,0));
        nameTextFiled = new JBTextField();
        nameTextFiled.setBackground(null);
        nameTextFiled.setBorder(new EmptyBorder(2,2,2,2));
        fieldBox.add(nameTextFiled);
        fieldBox.setBorder(new MatteBorder(1,0,1,0,new Color(100,100,100)));
        fieldBox.setBackground(new Color(60,63,65));

        box = new JPanel(new VerticalFlowLayout(0,0));
        box.add(fieldBox);

        String title = "";
        String msg = "以字母开头，且只包含小写字母、数字、下划线";
        NodeType nodeType = PathUtils.getNodeType(path);
        switch (nodeType){
            case business:
                pattern = "[a-z][a-z0-9_]{0,}[a-z0-9]";
                title = "添加项目";
                break;
            case project:
                pattern = "v[0-9]{1,5}";
                title = "添加版本";
                msg = "以v开头，以数字结尾";
                break;
            case version:
                pattern = "[a-z][a-z0-9_]{0,}[a-z0-9]";
                title = "添加模块";
                break;
            default:
                return MethodResult.fail(ResultCode.ERROR);
        }
        JBLabel label = new JBLabel(msg);
        label.setBorder(new EmptyBorder(5,5,5,5));
        label.setForeground(Color.GRAY);
        box.add(label);

        nameTextFiled.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if(e.getKeyChar()!=KeyEvent.VK_ENTER)
                    return;

               if(isMatch()){
                   popup.cancel();
               }
            }
        });

        popup = JBPopupFactory.getInstance().createComponentPopupBuilder(box,nameTextFiled).setTitle(title)
                .setFocusable(true)
                .setMovable(true)
                .setRequestFocus(true).setMinSize(new Dimension(360,0))
                .setProject(project)
                .createPopup();

        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                JBPopupListener.super.onClosed(event);
                String name = nameTextFiled.getText();
                if(StringUtils.isBlank(name))
                    return;
                if(!isMatch()){
                    return;
                }
                String baseFolder = Config.getInstance(project).getProtoBaseFolder();
                if(baseFolder==null){
                    NotifyService.error( project,"目前项目不支持gRPC，无法创建");
                }

                ApplicationManager.getApplication().runWriteAction(()->{
                    String newpath = baseFolder.concat(path).concat("/").concat(name);
                    //如果创建的是项目，直接创建一个版本
                    if(nodeType.equals(NodeType.business)){
                        newpath = newpath.concat("/v1");
                    }
                    PsiService.createAndGetDirectory(project, newpath);
                    callback.accept(name);
                });
            }
        });

        popup.showCenteredInCurrentWindow(project);//.showInFocusCenter();
        return MethodResult.success();
    }

    MatteBorder errorBorder = new MatteBorder(2,2,2,2,new Color(114,82,82));

    public boolean isMatch(){
        String name = nameTextFiled.getText();
        if(StringUtils.isBlank(name)) {
            nameTextFiled.setBorder(errorBorder);
            return false;
        }

        boolean isMatch = Pattern.matches(pattern, name);
        if(!isMatch){
            nameTextFiled.setBorder(errorBorder);
            return false;
        }
        nameTextFiled.setBorder(new EmptyBorder(2,2,2,2));
        return true;
    }

}
