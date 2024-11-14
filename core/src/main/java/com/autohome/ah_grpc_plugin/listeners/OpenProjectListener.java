package com.autohome.ah_grpc_plugin.listeners;

import com.autohome.ah_grpc_plugin.Config;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import org.jetbrains.annotations.NotNull;

public class OpenProjectListener implements ToolWindowManagerListener {

    @Override
    public void toolWindowShown(@NotNull ToolWindow toolWindow) {
        if (!toolWindow.getId().equals("Project")) return;
    }

}
