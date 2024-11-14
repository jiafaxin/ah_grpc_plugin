package com.autohome.ah_grpc_plugin.ui.menus;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.models.PermissionDto;
import com.autohome.ah_grpc_plugin.models.TreeItem;
import com.autohome.ah_grpc_plugin.services.AuthService;
import com.autohome.ah_grpc_plugin.services.CreateProtoService;
import com.autohome.ah_grpc_plugin.services.GitlabApiService;
import com.autohome.ah_grpc_plugin.services.NotifyService;
import com.autohome.ah_grpc_plugin.utils.AutohomeIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBDimension;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import java.util.function.Function;

public class CreateProtoMenu{

    JMenuItem menuItem;


    public JMenuItem create(Project project, TreeItem treeItem, DefaultMutableTreeNode node, Consumer<String> callback){
        menuItem = new JMenuItem("创建契约文件(.proto)", AutohomeIcons.ProtoFile);
        menuItem.addActionListener((e)->{

            if (!GitlabApiService.loginIfNot(project)) {
                return;
            }
            PermissionDto permission = AuthService.getInstance(project).hasPermission(treeItem.getPath(), GitlabApiService.getCurrentUser());
            if (!permission.isHasPermission()) {
                if (permission.getOwners() == null || permission.getOwners().size() == 0) {
                    NotifyService.error(project, "当前项目还未设置权限，请先找项目负责人设置权限后，再进行操作");
                } else {
                    NotifyService.error(project, "您没有对当前项目进行操作的权限，请先找项目负责人" + String.join(",", permission.getOwners()) + "添加权限后再进行操作");
                }
                return;
            }

            new CreateProtoService().create(project,treeItem.getPath(),(x)->{
                callback.accept(x);
            });
        });
        return menuItem;
    }
}
