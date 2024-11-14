//package com.autohome.ah_grpc_plugin.ui.menus;
//
//import com.autohome.ah_grpc_plugin.services.GitLocalService;
//import com.intellij.icons.AllIcons;
//import com.intellij.openapi.project.Project;
//
//import javax.swing.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//
//public class GitPushMenu extends JMenuItem {
//
//    String path;
//
//    public GitPushMenu(String _path, Project project){
//        super("推送到契约大仓", AllIcons.Actions.StepOut);
//        this.path = _path;
//        super.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                GitLocalService.getInstance(project).push(path);
//            }
//        });
//    }
//}
