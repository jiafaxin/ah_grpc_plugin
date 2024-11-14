package com.autohome.ah_grpc_plugin.factory;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.services.GrpcConfigService;
import com.autohome.ah_grpc_plugin.ui.MainToolwindow;
import com.autohome.ah_grpc_plugin.utils.AutohomeIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;

/**
 * 主窗口
 */
public class MainFactory implements ToolWindowFactory {

    final static String displayName = "";

    @Override
    public @Nullable Icon getIcon() {
        return AutohomeIcons.ArtifactSmallDark;
    }


    //默认不显示，等项目启动完成后，看是否显示
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        String path = project.getBasePath() + "/grpc.xml";
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(new File(path));
        if(null != virtualFile){
            return true;
        }
        return false;
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        Content content = contentFactory.createContent(MainToolwindow.getInstance(project), displayName, false);
        toolWindow.getContentManager().addContent(content);
    }

    public static Content getContent(Project project){
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(Config.toowindowId);
        if(toolWindow == null)
            return null;
        Content content = toolWindow.getContentManager().findContent(displayName);
        if(content == null)
            return null;
        return content;
    }

}
