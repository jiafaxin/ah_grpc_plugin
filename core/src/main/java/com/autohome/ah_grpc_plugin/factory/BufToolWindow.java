package com.autohome.ah_grpc_plugin.factory;

import com.autohome.ah_grpc_plugin.services.GrpcConfigService;
import com.autohome.ah_grpc_plugin.ui.ConsoleViewPanel;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 编译窗口
 */
public class BufToolWindow implements ToolWindowFactory  {
    public final static String ID = "autohomegrpcbuf";
    static final String CompileContentName = "编译/运行";
//    static final String BufContentName = "契约规范检查";
//    static final String GitGenContentName = "推送";

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return GrpcConfigService.getInstance(project).getSupportGrpc();
    }

    public static void beAvailable(Project project) {
        ToolWindowManager.getInstance(project).getToolWindow(ID).setAvailable(true);
    }

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        toolWindow.setStripeTitle("AUTOHOME gRPC");
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
//        toolWindow.getContentManager().addContent(createBufContent(toolWindow));
        toolWindow.getContentManager().addContent(createCompileContent(toolWindow));
//        toolWindow.getContentManager().addContent(createGitContent(toolWindow));
    }

//    Content createBufContent(ToolWindow toolWindow){
//        JComponent consolePanel = new ConsoleViewPanel(toolWindow.getProject());
//        Content content = toolWindow.getContentManager().getFactory().createContent(consolePanel, BufContentName, false);
//        return content;
//    }


//    Content createGitContent(ToolWindow toolWindow){
//        JComponent consolePanel = new ConsoleViewPanel(toolWindow.getProject());
//        Content content = toolWindow.getContentManager().getFactory().createContent(consolePanel, GitGenContentName, false);
//        return content;
//    }

    Content createCompileContent(ToolWindow toolWindow){
        JComponent consolePanel = new ConsoleViewPanel(toolWindow.getProject());
        Content content = toolWindow.getContentManager().getFactory().createContent(consolePanel, CompileContentName, false);
        return content;
    }

    public static ConsoleView getAndShowCompileView(Project project) {
        return getAndShow(project,CompileContentName);
    }

    public static ConsoleView getAndShowBufView(Project project) {
        return getAndShowCompileView(project);
//        return getAndShow(project,BufContentName);
    }
    public static ConsoleView getAndShowGitView(Project project) {
        return getAndShowCompileView(project);
//        return getAndShow(project,GitGenContentName);
    }

    public static ConsoleView getAndShow(Project project,String name){
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ID);
        toolWindow.show();
        ContentManager manager = toolWindow.getContentManager();
        manager.setSelectedContent(manager.findContent(name));
        Content content = manager.findContent(name);
        ConsoleViewPanel panel = (ConsoleViewPanel)content.getComponent();
        ConsoleView view = panel.getView();
        view.clear();
        return view;
    }


}
