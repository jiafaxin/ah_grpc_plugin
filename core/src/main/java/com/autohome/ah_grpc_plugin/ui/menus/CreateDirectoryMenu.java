package com.autohome.ah_grpc_plugin.ui.menus;

import com.autohome.ah_grpc_plugin.enums.NodeType;
import com.autohome.ah_grpc_plugin.models.PermissionDto;
import com.autohome.ah_grpc_plugin.models.TreeItem;
import com.autohome.ah_grpc_plugin.services.AuthService;
import com.autohome.ah_grpc_plugin.services.DirectoryService;
import com.autohome.ah_grpc_plugin.services.GitlabApiService;
import com.autohome.ah_grpc_plugin.services.NotifyService;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class CreateDirectoryMenu extends JMenuItem {

    public CreateDirectoryMenu(Project project, String name, TreeItem treeItem, Consumer<String> callback) {

        super(name, AllIcons.Actions.NewFolder);
        super.addActionListener(e -> {
            if (!GitlabApiService.loginIfNot(project)) {
                return;
            }
            if(treeItem.getNodeType().equals(NodeType.version) || treeItem.getNodeType().equals(NodeType.service)) {
                PermissionDto permission = AuthService.getInstance(project).hasPermission(treeItem.getPath(), GitlabApiService.getCurrentUser());
                if (!permission.isHasPermission()) {
                    if (permission.getOwners() == null || permission.getOwners().size() == 0) {
                        NotifyService.error(project, "当前项目还未设置权限，请先找项目负责人设置权限后，再进行操作");
                    } else {
                        NotifyService.error(project, "您没有对当前项目进行操作的权限，请先找项目负责人" + String.join(",", permission.getOwners()) + "添加权限后再进行操作");
                    }
                    return;
                }
            }
            new DirectoryService().create(project, treeItem.getPath(), (newDirectoryName) -> {
                callback.accept(newDirectoryName);
            });
        });
    }
}
