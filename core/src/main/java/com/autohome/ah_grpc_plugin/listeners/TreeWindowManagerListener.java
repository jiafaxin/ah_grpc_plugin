package com.autohome.ah_grpc_plugin.listeners;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.services.GrpcConfigService;
import com.autohome.ah_grpc_plugin.ui.MainToolwindow;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import org.jetbrains.annotations.NotNull;

public class TreeWindowManagerListener implements ToolWindowManagerListener {
    @Override
    public void toolWindowShown(@NotNull ToolWindow toolWindow) {
        if(!GrpcConfigService.getInstance(toolWindow.getProject()).getSupportGrpc()){
            return;
        }

        if (!toolWindow.getId().equals(Config.toowindowId)) {
            return;
        }

        MainToolwindow.getInstance(toolWindow.getProject()).showUpdate();
    }
}
