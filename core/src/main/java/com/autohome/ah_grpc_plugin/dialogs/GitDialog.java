package com.autohome.ah_grpc_plugin.dialogs;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.enums.GitLocalInfoType;
import com.autohome.ah_grpc_plugin.factory.BufToolWindow;
import com.autohome.ah_grpc_plugin.models.*;
import com.autohome.ah_grpc_plugin.services.*;
import com.autohome.ah_grpc_plugin.ui.MainToolwindow;
import com.autohome.ah_grpc_plugin.utils.GitTreeCellRenderer;
import com.autohome.ah_grpc_plugin.utils.JsonUtils;
import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.OptionAction;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.*;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Git 推送
 */
public class GitDialog extends DialogWrapper {
    List<GitAction> actions;

    JBTextArea textArea;

    Tree changeList;

    String path;

    JBLabel message;

    private Action myMainAction;

    DialogWrapperAction mainAction;

//    List<CompletableFuture> tasks;
    CompletableFuture<Boolean> bufTask;
    Consumer<Boolean> callback;

    ConsoleView consoleView;

    Project project;

    boolean forceType = false;

    public GitDialog(Project project, List<GitAction> _actions, String _path,ConsoleView consoleView, Consumer<Boolean> callback) {
        super(project);
        setTitle("推送当前项目的所有契约文件到大仓");
        this.project = project;
        this.actions = _actions;
        init();
        setSize(800, 600);
        path = _path;
        this.callback = callback;
        this.consoleView = consoleView;
//        tasks = new ArrayList<>();
//        for (GitAction action : this.actions) {
//            tasks.add(CompletableFuture.supplyAsync(() -> {
//                if (action.getFileName().equals("README.md")) {
//
//                } else {
//                    action.setContent(FileService.getFileContent(project, action.getFile_path()));
//                }
//
//                FileDetail fileDetail = GitlabApiService.getFile(project, action.getFile_path());
//                if (fileDetail == null || fileDetail.notExists()) {
//                    action.setAction(GitLocalInfoType.create);
//                } else {
//                    if(action.getContent().equals(fileDetail.getContent())) {
//                        action.setAction(GitLocalInfoType.none);
//                    }else{
//                        action.setAction(GitLocalInfoType.update);
//                    }
//                }
//                return true;
//            }));
//        }

        bufTask = CommandService.buf(project,consoleView).thenApply(pass -> {
            return pass;
        });
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainBody = new JPanel(new BorderLayout(0,10));
        mainBody.add(createChangeTree(),BorderLayout.CENTER);
        mainBody.add(createTextArea(),BorderLayout.SOUTH);
        return mainBody;
    }

    @Override
    protected JComponent createSouthPanel() {
        message = new JBLabel("");
        JComponent southPanel = super.createSouthPanel();
        JPanel panel = new JPanel(new GridLayout(1,1));
        JBSplitter splitter = new JBSplitter(false, 0.5F);
        splitter.setFirstComponent(message);
        splitter.setSecondComponent(southPanel);
        panel.add(splitter);
        return panel;
    }

    private JComponent createInfo() {
        JBPanel panel = new JBPanel(new VerticalFlowLayout());
        JBLabel label = new JBLabel(AnimatedIcon.Default.INSTANCE);
        label.setText("正在进行代码检查");

        panel.add(label);
        return panel;
    }

    private JComponent createChangeTree(){
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new ChangeTreeItem("autohome rpc",""));
        changeList = new Tree(root);
        changeList.setCellRenderer(new GitTreeCellRenderer());
        JBScrollPane firstComponent = new JBScrollPane(changeList);
        firstComponent.setBorder(null);
        for (GitAction action : this.actions) {
            changePathToNode(root,action.getFile_path().substring(13));
        }
        //展开所有节点
        expandAll(changeList,new TreePath(root),true);
        return firstComponent;
    }

    private JComponent createTextArea(){
        textArea = new JBTextArea();
        textArea.setBorder(new EmptyBorder(10,10,10,10));
        textArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        textArea.setRows(5);
        JBScrollPane textAreaScrollPane = new JBScrollPane(textArea);
        textAreaScrollPane.setBorder(null);
        return textAreaScrollPane;
    }


    public void changePathToNode(DefaultMutableTreeNode node,String path){
        String[] pss = path.split("/");
        String newPath = "";
        DefaultMutableTreeNode lastNode = node;
        for (String p : pss) {
            if(StringUtils.isNotBlank(newPath)){
                newPath = newPath+"/";
            }
            newPath = newPath + p;
            DefaultMutableTreeNode cNode = null;
            for (int i = 0; i < lastNode.getChildCount(); i++) {
                DefaultMutableTreeNode c = (DefaultMutableTreeNode)lastNode.getChildAt(i);
                ChangeTreeItem uo = (ChangeTreeItem)c.getUserObject();
                if(uo.getName().equals(p)){
                    cNode = c;
                    break;
                }
            }
            if(cNode==null){
                ChangeTreeItem item = new ChangeTreeItem(newPath);
                if(item.getName().equals("README.md")){
                    continue;
                }
                cNode = new DefaultMutableTreeNode(new ChangeTreeItem(newPath));
                lastNode.add(cNode);
            }
            lastNode = cNode;
        }
    }

    void expandAll(JTree tree, TreePath parent, boolean expand) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }
        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }

    @Override
    protected void doOKAction() {
        String msg = this.textArea.getText();
        if (StringUtils.isBlank(msg)) {
            message.setText("请输入推送说明");
            message.setForeground(new Color(255, 107, 104));
            this.callback.accept(false);
            return;
        }

        //先验证登录，后续在异步线程内无法进行登录
        GitlabApiService.loginIfNot(project);
        //强制推送
        if (forceType) {
            if (new Confim(project, "强制推送", "确定强制推送到契约大仓吗？").showAndGet()) {
                pushGoGen(consoleView);
                push();
                this.callback.accept(true);
                this.close(DialogWrapper.OK_EXIT_CODE);
            }
            return;
        }
        //等待buf检查和内容填充完成
        bufTask.thenApply(bufpass -> {
            actions.removeIf(x -> StringUtils.isBlank(x.getContent()));
            actions.removeIf(x -> x.getAction().equals(GitLocalInfoType.none));
            if (!bufpass) {
                NotifyService.error(project, "推送失败：代码检查未通过，请查看控制台输出，解决问题后再重提推送");
            }
            return bufpass;
        }).thenCompose(pass -> {
            //如果通过，则推送存根大仓
            if (pass) {
                return pushGoGen(consoleView);
            }
            return CompletableFuture.completedFuture(false);
        }).thenApply(pass -> {
            //如果通过，则推送契约大仓
            if (pass) {
                return push();
            }
            return false;
        }).thenAccept((pass) -> {
            if (pass) {
                this.callback.accept(true);
            } else {
                this.callback.accept(false);
            }
        });
        this.close(DialogWrapper.OK_EXIT_CODE);
    }

    CompletableFuture<Boolean> pushGoGen(ConsoleView consoleView) {
        if (!Config.getInstance(project).isGo()) {
            return CompletableFuture.completedFuture(true);
        }
        List<String> paths = new ArrayList<>();
        for (GitAction action : actions) {
            paths.add(action.getFile_path());
        }
        return GenProtoService.getInstance(project).compile(paths,consoleView).thenApply(success->{
            if(success){
                return GenProtoService.getInstance(project).push(consoleView,this.textArea.getText());
            }
            return false;
        });
    }

    boolean push() {
        consoleView.print("开始推送 \n", ConsoleViewContentType.NORMAL_OUTPUT);
        GitPushList pushList = new GitPushList();
        pushList.setActions(actions);
        pushList.setCommit_message(this.textArea.getText());
//        //提交
//        Integer code = GitService.getInstance(project).push(pushList).join();
//        if(code != 0){
//            switch (code) {
//                case -1:
//                    NotifyService.error(project,"推送失败：您需要登录后才能提交");
//                    break;
//                case 401:
//                    NotifyService.error(project,"推送失败：账号登录失败，请登录后重新推送");
//                    break;
//            }
//            return false;
//        }
        ApiResult<GitCommitResult> commitResult = GitlabApiService.commit(project,pushList).join();
        if (commitResult.getCode() != 0 && commitResult.getCode()!=201) {
            switch (commitResult.getCode()) {
                case -1:
                    consoleView.print("推送失败：您需要登录后才能提交", ConsoleViewContentType.ERROR_OUTPUT);
                    break;
                case 401:
                    consoleView.print("推送失败：账号登录失败，请登录后重新推送", ConsoleViewContentType.ERROR_OUTPUT);
                    break;
                case 403:
                    consoleView.print("推送失败：账号推送权限不足，请找管理员添加主程序员权限", ConsoleViewContentType.ERROR_OUTPUT);
                    break;
                default:
                    NotifyService.error(project,commitResult.getMsg());
                    break;
            }
            return false;
        }
        if (StringUtils.isBlank(commitResult.getResult().getId())) {
            consoleView.print("推送失败：" + commitResult.getResult().getMessage(), ConsoleViewContentType.ERROR_OUTPUT);
            return false;
        } else {
            List<String> paths = actions.stream().map(x -> x.getFile_path()).collect(Collectors.toList());
            //移除所有更改，重新刷新sha256
            FileChangeLog.getInstance(project).removeList(paths);
            MainToolwindow.getInstance(project).refreshStatus();
            consoleView.print("推送成功：共推送更改 " + actions.size() + " 个文件 ", ConsoleViewContentType.LOG_WARNING_OUTPUT);
            return true;
        }
    }

    @Override
    protected Action @NotNull [] createActions() {
        mainAction = new DialogWrapperAction("检查代码并推送") {
            @Override
            protected void doAction(ActionEvent e) {
                doOKAction();
            }
        };
        Action sAction = new DialogWrapperAction("强制推送") {
            @Override
            protected void doAction(ActionEvent e) {
                forceType = true;
                doOKAction();
            }
        };


        mainAction.setEnabled(false);

        myMainAction = new GitOptionAction("检查代码并推送", mainAction, new Action[]{sAction});
        final List<Action> actions = new ArrayList<>();
        myMainAction.putValue(DEFAULT_ACTION, Boolean.TRUE);
        actions.add(myMainAction);
        actions.add(getCancelAction());
        return actions.toArray(new Action[0]);
    }

    @Override
    protected @NotNull Action getOKAction() {
        return myMainAction;
    }

    private static final class GitOptionAction extends AbstractAction implements OptionAction{
        Action mainAction;
        private final Action[] myAdditionalActions;
        GitOptionAction(String text, Action _mainAction,Action[] options) {
            super(text);
            mainAction = _mainAction;
            myAdditionalActions=options;
        }

        public void setMainAction(Action _mainAction){
            mainAction = _mainAction;
        }

        @Override
        public Action @NotNull [] getOptions() {
            return myAdditionalActions;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            mainAction.actionPerformed(e);
        }
    }

}
