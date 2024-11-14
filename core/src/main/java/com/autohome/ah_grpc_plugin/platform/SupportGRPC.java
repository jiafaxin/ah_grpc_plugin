package com.autohome.ah_grpc_plugin.platform;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;

public interface SupportGRPC {


    /**
     * @param project 项目
     */
    boolean support(Project project);


    boolean support(Project project, AnActionEvent e, String currPath);

    boolean supportGrpc(Project project, AnActionEvent e, String currPath,boolean isConsumer);

}
