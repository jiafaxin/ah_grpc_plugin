package com.autohome.ah_grpc_plugin.actions;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.factory.BufToolWindow;
import com.autohome.ah_grpc_plugin.platform.SupportGRPC;
import com.autohome.ah_grpc_plugin.providers.services.ImplProtoService;
import com.autohome.ah_grpc_plugin.providers.services.ProtoImplService;
import com.autohome.ah_grpc_plugin.services.GenProtoService;
import com.autohome.ah_grpc_plugin.services.GitLocalService;
import com.autohome.ah_grpc_plugin.services.GrpcConfigService;
import com.autohome.ah_grpc_plugin.services.NotifyService;
import com.autohome.ah_grpc_plugin.ui.MainToolwindow;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ProviderSupportGrpc extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setVisible(true);
    }
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        SupportGRPC supportGRPC = Config.getInstance(e.getProject()).platformConfig().getSuppprtGRPC();
        VirtualFile currentFolder = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        String currentPath = currentFolder.getPath();
        boolean isGo = Config.getInstance(project).isGo();
        //go项目
        if(isGo){
            if (supportGRPC.supportGrpc(e.getProject(), e, currentPath,false)) {
                ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(Config.toowindowId);
                window.setAvailable(true);
                BufToolWindow.beAvailable(project);
                MainToolwindow.getInstance(project).refreshRoot();
                //安装 gRPC 工具包 依赖,检查go环境
                GenProtoService.getInstance(project).goProviderCheck();
            }
            return;
        }
        //java
        if(supportGRPC.supportGrpc(e.getProject(), e, currentPath,false)) {

            ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(Config.toowindowId);
            window.setAvailable(true);
            BufToolWindow.beAvailable(project);
            MainToolwindow.getInstance(project).refreshRoot();

            //扫描所有本地文件，把服务器端不存在的文件，添加到大仓git提交记录
//            GitLocalService.getInstance(e.getProject()).scanAll();
            ProtoImplService.getInstance(project).init();
            ImplProtoService.getInstance(project).init();
        }

    }
}
