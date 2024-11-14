//package com.autohome.ah_grpc_plugin.actions;
//
//import com.autohome.ah_grpc_plugin.Config;
//import com.autohome.ah_grpc_plugin.factory.BufToolWindow;
//import com.autohome.ah_grpc_plugin.platform.SupportGRPC;
//import com.autohome.ah_grpc_plugin.providers.services.ImplProtoService;
//import com.autohome.ah_grpc_plugin.providers.services.ProtoImplService;
//import com.autohome.ah_grpc_plugin.services.GitLocalService;
//import com.autohome.ah_grpc_plugin.services.GrpcConfigService;
//import com.autohome.ah_grpc_plugin.ui.MainToolwindow;
//import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
//import com.intellij.openapi.actionSystem.AnAction;
//import com.intellij.openapi.actionSystem.AnActionEvent;
//import com.intellij.openapi.actionSystem.CommonDataKeys;
//import com.intellij.openapi.module.Module;
//import com.intellij.openapi.module.ModuleManager;
//import com.intellij.openapi.project.Project;
//import com.intellij.openapi.vfs.VirtualFile;
//import com.intellij.openapi.wm.ToolWindow;
//import com.intellij.openapi.wm.ToolWindowManager;
//import com.intellij.pom.Navigatable;
//import org.jetbrains.annotations.NotNull;
//
//
///**
// * 单模块项目支持gRPC
// */
//public class SupportSingleGrpc extends AnAction {
//
//    @Override
//    public void update(@NotNull AnActionEvent e) {
//        e.getPresentation().setVisible(false);
//
//        Navigatable[] navigatables = e.getData(CommonDataKeys.NAVIGATABLE_ARRAY);
//        if (navigatables == null)
//            return;
//        if (navigatables.length == 0)
//            return;
//
//        Project project = e.getProject();
//        Module[] modules = ModuleManager.getInstance(project).getModules();
//        if (modules.length > 1)
//            return;
//        Navigatable navigatable = navigatables[0];
//        if (!(navigatable instanceof PsiDirectoryNode)) {
//            return;
//        }
//        PsiDirectoryNode directory = (PsiDirectoryNode) navigatable;
//
//        VirtualFile file = directory.getVirtualFile();
//
//        if (!file.getPath().equals(e.getProject().getBasePath())) {
//            return;
//        }
//
//        e.getPresentation().setVisible(true);
//    }
//
//    @Override
//    public void actionPerformed(@NotNull AnActionEvent e) {
//        Project project = e.getProject();
//        SupportGRPC supportGRPC = Config.getInstance(e.getProject()).platformConfig().getSuppprtGRPC();
//        if(supportGRPC.support(e.getProject(), e, "")) {
//            GrpcConfigService.getInstance(project).support("");
//
//            ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(Config.toowindowId);
//            window.setAvailable(true);
//            BufToolWindow.beAvailable(project);
//            MainToolwindow.getInstance(project).refreshRoot();
//
//            //扫描所有本地文件，把服务器端不存在的文件，添加到大仓git提交记录
////            GitLocalService.getInstance(e.getProject()).scanAll();
//            ProtoImplService.getInstance(project).init();
//            ImplProtoService.getInstance(project).init();
//        }
//    }
//}
