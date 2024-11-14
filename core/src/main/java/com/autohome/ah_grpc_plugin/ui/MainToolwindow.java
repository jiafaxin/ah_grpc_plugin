package com.autohome.ah_grpc_plugin.ui;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.dialogs.Confim;
import com.autohome.ah_grpc_plugin.dialogs.SearchShow;
import com.autohome.ah_grpc_plugin.enums.FileFrom;
import com.autohome.ah_grpc_plugin.enums.GitLocalInfoType;
import com.autohome.ah_grpc_plugin.enums.NodeType;
import com.autohome.ah_grpc_plugin.factory.MainFactory;
import com.autohome.ah_grpc_plugin.models.*;
import com.autohome.ah_grpc_plugin.models.gitlab.ProtoTreeNode;
import com.autohome.ah_grpc_plugin.services.*;
import com.autohome.ah_grpc_plugin.ui.menus.*;
import com.autohome.ah_grpc_plugin.utils.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl;
import com.intellij.protobuf.lang.PbFileType;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.treeStructure.Tree;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MainToolwindow  extends SimpleToolWindowPanel{
    public synchronized static MainToolwindow getInstance(Project project) {
        Content content = MainFactory.getContent(project);
        if (content == null)
            return new MainToolwindow(project);
        return (MainToolwindow) content.getComponent();
    }

    public MainToolwindow(Project project){
        super(true,true);
        this.project = project;
        init("");
    }
    private JTree defaultTree;
    DefaultMutableTreeNode baseNode;
    Project project;

    String baseNodePath = "autohome/rpc";

    AnAction loginAction;

    AnAction logoutAction;

    boolean opening = false;

    JPanel updatePanel;

    public void init(String start) {
        JBPanel center = new JBPanel(new BorderLayout(0, 0));
        center.add(createSearch(), BorderLayout.NORTH);
        center.add(createTree(), BorderLayout.CENTER);
        //tree
        add(center, BorderLayout.CENTER);
        add(createSouth(), BorderLayout.SOUTH);
        setToolbar(createToolbar());
        try {
            refreshRoot();
        } catch (Exception e) {
            NotifyService.error(project, "拉取大仓失败，请刷新重试:" + e.getMessage());
        }
    }

    JComponent createSouth(){

        JPanel south = new JPanel(new VerticalFlowLayout(0,0));
        south.setBackground(null);

        JPanel southNorth = new JPanel(new VerticalFlowLayout(5,5));
        southNorth.setBackground(null);

//        JButton btn = new JButton("推送所有变更到契约大仓", AllIcons.Actions.StepOut);
//        btn.addActionListener((e)->{
//            GitLocalService.getInstance(project).push("");
//        });
//
//        southNorth.add(btn);
        southNorth.add(createActions());

        updatePanel = new JPanel(new BorderLayout(0,0));
        updatePanel.setVisible(false);
        updatePanel.add(new JBLabel("发现新版本，强烈建议您立即升级！"),BorderLayout.CENTER);
        JButton updateButton = new JButton("立即升级");
        updateButton.addActionListener((e)->{
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "Plugins");
            if(!UpdateService.needUpdate(project)) {
                updatePanel.setVisible(false);
            }
        });
        updateButton.setBackground(Backgrounds.error);
        updatePanel.add(updateButton,BorderLayout.EAST);
        updatePanel.setBorder(new EmptyBorder(5,10,5,5));
        updatePanel.setBackground(Backgrounds.error);
        south.add(southNorth);
        south.add(updatePanel);

        return south;
    }

    public void showUpdate(){
        if(UpdateService.needUpdate(project)) {
            updatePanel.setVisible(true);
        }else{
            updatePanel.setVisible(false);
        }
    }

    JComponent createToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup("MainToolWindowGroup", false);
        actionGroup.add(new AnAction("刷新根目录", "", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                refreshRoot();
                NotifyService.info(project,"刷新完成");
            }
        });

        actionGroup.add(new AnAction("定位当前文件", "", AllIcons.General.Locate) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                FileEditor editor = FileEditorManager.getInstance(project).getSelectedEditor();
                if(editor==null)
                    return;
                VirtualFile file = editor.getFile();
                if (file == null)
                    return;
                if (!(file.getFileType() instanceof PbFileType))
                    return;

                ProtoPath protoPath = ProtoPath.newInstance(file.getPath());
                if(protoPath==null)
                    return;

                openPath(protoPath, baseNodePath, NodeType.root, baseNode, new TreePath(baseNode));
            }
        });

        actionGroup.add(new AnAction("展开全部节点", "", AllIcons.Actions.Expandall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TreeUtil.expandAll(defaultTree,getTreePath(baseNode),true);
                defaultTree.updateUI();
            }
        });

        actionGroup.add(new AnAction("关闭全部节点", "", AllIcons.Actions.Collapseall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TreeUtil.expandAll(defaultTree,getTreePath(baseNode),false);
                defaultTree.updateUI();
            }
        });

        loginAction = new AnAction("登录","",AllIcons.CodeWithMe.CwmAccess) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if(GitlabApiService.login(project)!=null){
                    actionGroup.add(logoutAction);
                    actionGroup.remove(loginAction);
                }
            }
        };

        logoutAction =  new AnAction("退出","",AllIcons.CodeWithMe.CwmAccessOn) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                GitlabApiService.logout();
                actionGroup.add(loginAction);
                actionGroup.remove(logoutAction);
            }
        };

        if(GitlabApiService.hasLogin()){
            actionGroup.add(logoutAction);
        }else{
            actionGroup.add(loginAction);
        }
        actionGroup.add(new AnAction("推送当前项目的所有契约文件到大仓", "", AllIcons.Vcs.Push) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                GitLocalService.getInstance(project).push();
            }
        });
        actionGroup.add(new AnAction("拉取其他proto文件", "", AllIcons.Vcs.Fetch) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                GrpcConfigService grpcConfigService = GrpcConfigService.getInstance(project);
                if(StringUtils.isBlank(grpcConfigService.getIdlPath())){
                    NotifyService.warning(project,"请先绑定项目后再拉取其他proto!");
                    return;
                }
                ApplicationManager.getApplication().invokeLater(() -> {
                    int page = 1;
                    while (loadAll(baseNodePath, page,grpcConfigService.getIdlPath())) {
                        page++;
                    }
                    NotifyService.info(project,"下载完成");
                    refreshStatus(baseNode);
                });
                NotifyService.info(project,"拉取其他proto文件成功!");
            }
        });

        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("MainTool", actionGroup, true);
        actionToolbar.setTargetComponent(actionToolbar.getComponent());
        return actionToolbar.getComponent();
    }

    public void login(){
        setToolbar(createToolbar());
    }

    JComponent createSearch(){
        JBTextField searchInput = new JBTextField();
        searchInput.setBorder(new EmptyBorder(5,0,5,5));
        searchInput.getEmptyText().setText("输入要搜索的内容或路径");
        searchInput.addActionListener(e -> {
            String keyword = searchInput.getText();
            ProtoPath path = ProtoPath.newInstance(keyword);
            if(path==null) {
                new SearchShow().search(project, e, searchInput.getText());
                searchInput.setText("");
            }else {
                openPath(path,baseNodePath,NodeType.root,baseNode,new TreePath(baseNode));
            }
        });

        JLabel icon = new JBLabel(AllIcons.Actions.Search);
        icon.setBorder(new EmptyBorder(5,5,5,0));
        icon.setBackground(searchInput.getBackground());
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(icon,BorderLayout.WEST);
        panel.add(searchInput,BorderLayout.CENTER);
        panel.setBackground(searchInput.getBackground());
        panel.setBorder(new MatteBorder(0,0,1,0, BorderColors.base));
        return panel;
    }

    void openPath(ProtoPath path,String prePath,NodeType nodeType,DefaultMutableTreeNode preNode,TreePath preTreePath){
        String nextPath = prePath;
        NodeType nextNodeType = null;
        switch (nodeType){
            case root:
                if(StringUtils.isBlank(path.getBusiness()))
                    return;
                nextPath += "/" + path.getBusiness();
                nextNodeType = NodeType.business;
                break;
            case business:
                if(StringUtils.isBlank(path.getProject()))
                    return;
                nextPath += "/" + path.getProject();
                nextNodeType = NodeType.project;
                break;
            case project:
                if(StringUtils.isBlank(path.getVersion()))
                    return;
                nextPath += "/" + path.getVersion();
                nextNodeType = NodeType.version;
                break;
            case version:
                if(StringUtils.isBlank(path.getModule()) && StringUtils.isBlank(path.getFileName()))
                    return;
                if(StringUtils.isBlank(path.getModule())){
                    nextPath += "/" + path.getFileName();
                    nextNodeType = NodeType.proto;
                }else{
                    nextPath += "/" + path.getModule();
                    nextNodeType = NodeType.service;
                }
                break;
            case service:
                if(StringUtils.isBlank(path.getFileName())){
                    return;
                }
                nextPath += "/" + path.getFileName();
                nextNodeType = NodeType.proto;
                break;
        }

        DefaultMutableTreeNode node = getChildByPath(preNode,nextPath);
        if(node==null)
            return;
        TreePath treePath = preTreePath.pathByAddingChild(node);
        if(nextNodeType.equals(NodeType.proto)){
            defaultTree.setSelectionPath(treePath);
        }else {
            openPath(path, nextPath, nextNodeType, node, treePath);
        }
        defaultTree.updateUI();
    }

    DefaultMutableTreeNode getChildByPath(DefaultMutableTreeNode preNode, String nextPath){
        for (int i = 0; i < preNode.getChildCount(); i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)preNode.getChildAt(i);
            TreeItem item = (TreeItem)node.getUserObject();
            if(item.getPath().equals(nextPath)) {
                return node;
            }
        }
        return null;
    }

    JComponent createActions() {
        JButton yj = new JButton("意见反馈");
        yj.addActionListener(e->{
            BrowserUtil.open("https://doc.autohome.com.cn/docapi/page/share/share_lieBXQOquW");
        });

        JButton by = new JButton("生成Stub");
        by.addActionListener(e->{
            boolean isGo = Config.getInstance(project).isGo();
            //走go编译，编译所有的proto生成pb文件
            if(isGo){
                FileDocumentManager.getInstance().saveAllDocuments();
                GenProtoService genProtoService = GenProtoService.getInstance(project);
                List<String> allProtoFilesPaths = genProtoService.getAllProtoFilesPaths("proto/autohome/rpc");
                genProtoService.compile(allProtoFilesPaths);
                return;
            }
            CommandService.protobufCompile(project);
        });

        JBPanel btns = new JBPanel(new FlowLayout(FlowLayout.RIGHT,0,0));
        btns.setBackground(null);
        btns.add(yj);

        JBPanel firPanel = new JBPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        firPanel.setBackground(null);
        firPanel.add(by);

        JBPanel panel = new JBPanel(new VerticalFlowLayout(0,0));
        JBSplitter splitter = new JBSplitter(false,0.1f);
        splitter.setFirstComponent(firPanel);
        splitter.setSecondComponent(btns);
        panel.add(splitter);
        panel.setBackground(null);

        return panel;
    }

    JComponent createTree() {

        //定义跟节点
        baseNode = new DefaultMutableTreeNode(new TreeItem() {{
            setType("tree");
            setPath(baseNodePath);
            setName("autohome rpc");
        }});

        defaultTree = new Tree();

        defaultTree.setCellRenderer(new GrpcTreeCellRenderer());

        //给tree添加菜单
        addMenu();

        //点击proto文件打开编辑器
        bindOpenFile();

        defaultTree.expandPath(new TreePath(baseNode));
        JBScrollPane scrollPane = new JBScrollPane(defaultTree);
        scrollPane.setBorder(null);

        return scrollPane;
    }

    public DefaultMutableTreeNode getNode(String path) {
        return getNode(baseNode,path);
    }

    public DefaultMutableTreeNode getNode(DefaultMutableTreeNode node, String path) {
        TreeItem treeItem = (TreeItem) node.getUserObject();
        if (path.equals(treeItem.getPath()))
            return node;
        if (path.indexOf(treeItem.getPath()) < 0)
            return null;
        if (node.getChildCount() > 0) {
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode node1 = getNode((DefaultMutableTreeNode) node.getChildAt(i), path);
                if (node1 != null) {
                    return node1;
                }
            }
        }
        return null;
    }

    public void refreshRoot(){
        //git 上的目录
        ProtoTreeNode rootNode = GitService.getInstance().getTree();

        //本地目录
        ProtoTreeNode vfNode = LocalProto.getInstance(project).get();

        baseNode = convert(rootNode,vfNode);
        DefaultTreeModel treeModel = new DefaultTreeModel(baseNode);
        defaultTree.setModel(treeModel);
    }

    public void refreshNode(DefaultMutableTreeNode node){
        node.removeAllChildren();
        defaultTree.updateUI();
        TreeItem treeItem = (TreeItem)node.getUserObject();
        ProtoTreeNode rootNode = GitService.getInstance().reloadTree(treeItem.getPath());
        ProtoTreeNode vfNode = LocalProto.getInstance(project).get("/"+treeItem.getPath());
        DefaultMutableTreeNode newNode = convert(rootNode,vfNode);
        while (newNode.getChildCount()>0){
            node.add((DefaultMutableTreeNode)newNode.getChildAt(0));
        }
        defaultTree.updateUI();
    }

    DefaultMutableTreeNode convert(ProtoTreeNode gitNode,ProtoTreeNode vfNode){

        TreeItem treeItem = new TreeItem();

        String idlPath = GrpcConfigService.getInstance(project).getIdlPath();

        if(gitNode!=null){  //节点可以来自git
            treeItem.setType(gitNode.getType());
            treeItem.setName(gitNode.getName());
            treeItem.setPath(gitNode.getPath());
            treeItem.setExistsLocal(vfNode!=null);
            treeItem.setFileFrom(FileFrom.GITLAB);
        }else if(vfNode!=null){  //节点也可以来自file
            treeItem.setType(vfNode.getType());
            treeItem.setName(vfNode.getName());
            treeItem.setPath(vfNode.getPath());
            treeItem.setExistsLocal(true);
            treeItem.setFileFrom(FileFrom.LOCAL);
        }

        if(idlPath.startsWith(treeItem.getPath())){
            treeItem.setBindProject(true);
        }


        DefaultMutableTreeNode node = new DefaultMutableTreeNode(treeItem,true);

        List<String> gitPaths = new ArrayList<>();
        if(gitNode!=null && gitNode.getChilds()!=null && gitNode.getChilds().size()>0) {
            for (ProtoTreeNode child : gitNode.getChilds()) {
                gitPaths.add(child.getPath());
                ProtoTreeNode vfChild = vfNode == null? null : vfNode.getChilds().stream().filter(x->x.getPath().equals(child.getPath())).findFirst().orElse(null);
                node.add(convert(child,vfChild));
            }
        }
        if(vfNode!=null && vfNode.getChilds()!=null && vfNode.getChilds().size()>0){
            for (ProtoTreeNode child : vfNode.getChilds()) {
                if(gitPaths.contains(child.getPath()))
                    continue;
                node.add(convert(null,child));
            }
        }
        return node;
    }

    public void refreshNode(DefaultMutableTreeNode node,boolean expand,boolean refreshChildren,TreePath nodeTreePath) {
        if (node == null)
            return;

        Object object = node.getUserObject();
        TreeItem item = (TreeItem) object;
        if (item.getType().equals("tree")) {
            java.util.List<DefaultMutableTreeNode> cs = getNodes(item.getPath());
            node.removeAllChildren();
            for (DefaultMutableTreeNode c : cs) {
                node.add(c);
            }
            if(expand && nodeTreePath !=null) {
                try {
                    defaultTree.expandPath(nodeTreePath);
                }catch (Exception e){
                }
            }
            TreePath treePath = nodeTreePath.pathByAddingChild(node);
            if(refreshChildren && cs.size()>0){
                for (DefaultMutableTreeNode c : cs) {
                    ProgressManager.getInstance().run(new Task.Backgroundable(project, "刷新节点") {
                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                            refreshNode(c,false,false,treePath);
                            defaultTree.updateUI();
                        }
                    });
                }
            }
        }
    }

    java.util.List<DefaultMutableTreeNode> getNodes(String path) {
        java.util.List<TreeItem> cs = getTreeItems(path);
        java.util.List<DefaultMutableTreeNode> list = new ArrayList<>();
        for (TreeItem c : cs) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(c, c.getType().equals("tree"));
            list.add(node);
        }
        return list;
    }

    java.util.List<TreeItem> getTreeItems(String path) {
        ApiResult<java.util.List<TreeItem>> csResult = GitlabApiService.getItems(project, path);
        if (csResult.getCode() != 0) {
            return new ArrayList<>();
        }
        java.util.List<TreeItem> cs = csResult.getResult();
        VirtualFile[] files = FileService.getAllFiles(project, path);
        java.util.List<String> allPaths = cs.stream().map(x -> x.getPath()).collect(Collectors.toList());

        for (VirtualFile file : files) {
            if(!(file instanceof VirtualDirectoryImpl) && !(file.getFileType() instanceof PbFileType)){
                continue;
            }
            String realPath = PathUtils.getProtoPath(project,file.getPath());
            if(realPath==null) continue;
            if (allPaths.contains(realPath)){
                cs.stream().filter(x->x.getPath().equals(realPath)).forEach(x->x.setExistsLocal(true));
                continue;
            }
            cs.add(new TreeItem() {{
                setType(PathUtils.getNodeType(realPath).equals(NodeType.proto) ? "blob" : "tree");
                setPath(realPath);
                setAction(GitLocalInfoType.create);
                setFileFrom(FileFrom.LOCAL);
                setName(realPath.substring(realPath.lastIndexOf("/") + 1));
            }});
        }

        List<GitAction> actions = GitLocalService.getInstance(project).getAllActions();
        for (TreeItem c : cs) {
            if(c.getFileFrom().equals(FileFrom.LOCAL))
                continue;
            List<GitAction> itemActions = actions.stream().filter(x->x.getFile_path().equals(c.getPath())).collect(Collectors.toList());
            if(itemActions.size()>0){
                c.setAction(GitLocalInfoType.update);
            }
        }
        return cs;
    }

    void addMenu() {
        defaultTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    int x = e.getX();
                    int y = e.getY();

                    TreePath path = defaultTree.getPathForLocation(x, y);
                    if (path == null) {
                        path = getPathForLocation(50, y);
                    }
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

                        //右键
                        if (SwingUtilities.isRightMouseButton(e)) {
                            JPopupMenu menu = rightMenu(node);
                            menu.show(defaultTree, x, y);
                            return;
                        }
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            if (node == null)
                                return;

                            Object object = node.getUserObject();
                            TreeItem item = (TreeItem) object;
                            if (item.getType().equals("blob")) {
                                //点击打开文件
                                VirtualFileService.open(project, item.getPath());
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        });
    }

    int maxLocationX = 240;

    TreePath getPathForLocation(int x, int y) {
        if (x > maxLocationX)
            return null;
        TreePath path = defaultTree.getPathForLocation(x, y);
        if (path == null) {
            x = x + 10;
            return getPathForLocation(x, y);
        }
        return path;
    }

    //选中事件：选中proto，打开fileEditor
    void bindOpenFile() {
        defaultTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) defaultTree.getLastSelectedPathComponent();

            if (node == null)
                return;

            Object object = node.getUserObject();
            TreeItem item = (TreeItem) object;
            if (item.getType().equals("blob")) {
                //点击打开文件
                VirtualFileService.open(project, item.getPath());
            }else if(item.getNodeType().equals(NodeType.project)){
//                ProjectConfigFile.getInstance(project).open(item.getPath());
            }
        });
    }

    void openAll(DefaultMutableTreeNode node,TreePath nodeTreePath){
        opening = true;
        //刷新当前节点
        refreshNode(node,true,false,nodeTreePath);
        if(node.getChildCount()>0){
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode cn = (DefaultMutableTreeNode)node.getChildAt(i);
                TreeItem item =(TreeItem) cn.getUserObject();
                if(item==null) continue;
                if(item.getType().equals("blob"))
                    continue;
                TreePath treePath = nodeTreePath.pathByAddingChild(cn);
                openAll(cn,treePath);
            }
        }
        opening = false;
    }

    TreePath getTreePath(DefaultMutableTreeNode node){
        return getTreePath(baseNode,node,new TreePath(baseNode));
    }

    TreePath getTreePath(DefaultMutableTreeNode parentNode, DefaultMutableTreeNode node,TreePath parentTreePath){
        TreeItem treeItem = (TreeItem)node.getUserObject();
        if(parentNode == node)
            return parentTreePath;
        for (int i = 0; i < parentNode.getChildCount(); i++) {
            DefaultMutableTreeNode cn = (DefaultMutableTreeNode) parentNode.getChildAt(i);
            if(cn.equals(node)){
                return parentTreePath.pathByAddingChild(cn);
            }
            TreeItem cnTreeItem = (TreeItem) cn.getUserObject();
            if(treeItem.getPath().indexOf(cnTreeItem.getPath()+"/") == 0){
                return getTreePath(cn,node,parentTreePath.pathByAddingChild(cn));
            }
        }
        return null;
    }


    JPopupMenu rightMenu(DefaultMutableTreeNode node) {

        JPopupMenu menu = new JPopupMenu();
        TreeItem treeItem = (TreeItem) node.getUserObject();
        if (treeItem == null)
            return menu;

        java.util.List<JMenuItem> createMenu = createNewMenu(treeItem, node);
        if (createMenu != null && createMenu.size() > 0) {
            for (JMenuItem it : createMenu) {
                menu.add(it);
            }
        }

        if (!treeItem.getNodeType().equals(NodeType.proto)) {
            menu.addSeparator();
            if (!treeItem.getNodeType().equals(NodeType.service)) {
                menu.add(new BaseMenuItem("展开全部子节点", () -> {
                    TreeUtil.expandAll(defaultTree,getTreePath(node),true);
                    defaultTree.updateUI();
                }));
                menu.add(new BaseMenuItem("关闭全部子节点", () -> {
                    TreeUtil.expandAll(defaultTree,getTreePath(node),false);
                    defaultTree.updateUI();
                }));
            }
            menu.add(new RefreshMenu(() -> {
                refreshNode(node);
                NotifyService.info(project,"刷新完成");
            }));
        }
//        menu.addSeparator();
//        menu.add(new GitPushMenu(treeItem.getPath(),project));

        if(treeItem.getNodeType().equals(NodeType.proto)) {
            if(treeItem.isExistsLocal()){
                JMenuItem updateFile = new JMenuItem("拉取", AllIcons.Actions.TraceInto);
                updateFile.addActionListener(e->{
                    ProtoFileService.reload(project, treeItem.getPath());
                });
                menu.add(updateFile);
            }else{
                JMenuItem pull = new JMenuItem("下载当前文件到本地", AllIcons.Actions.TraceInto);
                pull.addActionListener(e->{
                    FileService.download(project, treeItem.getPath());
                    refreshStatus(node);
                });
                menu.add(pull);
            }
        }else{
            menu.add(downloadAll(treeItem.getPath(), node));
        }
        //绑定到当前项目
        if(treeItem.getNodeType().equals(NodeType.project)){
            menu.addSeparator();
            menu.add(bindMenu(node, treeItem));
        }

        if(treeItem.getNodeType().equals(NodeType.project)){
            menu.addSeparator();
            BaseMenuItem setAuth = new BaseMenuItem("查看与编辑权限",()->{
                ProtoPath path = ProtoPath.newInstance(treeItem.getPath());
                if(path != null && path.isProject()){
                    ConfigProjectFile.getInstance(project).open(path.realPath());
                }
            });
            menu.add(setAuth);
        }


        if(!treeItem.getNodeType().equals(NodeType.root)) {
            menu.addSeparator();
            menu.add(deleteMenu(node, treeItem));
        }

        if(treeItem.getNodeType().equals(NodeType.project)) {
            menu.addSeparator();
            menu.add(projectConfig(treeItem.getPath(), node));
        }

        return menu;
    }



    JMenuItem downloadAll(String path,DefaultMutableTreeNode node) {
        JMenuItem menu = new JMenuItem("下载此文件夹下的所有文件到本地", AllIcons.Actions.TraceInto);
        menu.addActionListener(e -> {

            ApplicationManager.getApplication().invokeLater(() -> {
                int page = 1;
                while (loadAll(path, page,"")) {
                    page++;
                }
                NotifyService.info(project,"下载完成");
                refreshStatus(node);
            });
        });
        return menu;
    }


    JMenuItem projectConfig(String path,DefaultMutableTreeNode node) {
        JMenuItem menu = new JMenuItem("编辑与查看配置");
        menu.addActionListener(e -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                ProjectConfigFile.getInstance(project).open(path);
            });
        });
        return menu;
    }

    boolean loadAll(String path,int page,String idlPath){
        ApiResult<List<TreeItem>> list = GitlabApiService.getItems(project, path,true,page);
        if(list.getCode()>300 || list.getResult()==null||list.getResult().size()==0)
            return false;
        list.getResult().forEach(item -> {
            if(!item.getType().equals("blob")) {
                return;
            }
            if(StringUtils.isNotBlank(idlPath) && item.getPath().contains(idlPath)){
                return;
            }
            FileService.download(project, item.getPath(),false);
        });
        return true;
    }




    JMenuItem deleteMenu(DefaultMutableTreeNode node, TreeItem treeItem) {
        JMenuItem menu = new JMenuItem("删除本地文件");
        menu.addActionListener(e -> {
            if(new Confim(project,"确认","删除后将不可恢复，确认要删除吗？").showAndGet()) {
                PsiService.deleteFile(project,Config.getInstance(project).protoBasePath() + treeItem.getPath(), () -> {
                    if(treeItem.getFileFrom().equals(FileFrom.LOCAL)){
                        node.removeFromParent();
                    }else{
                        refreshStatus(node);
                    }
                    defaultTree.updateUI();
                });
            }
        });
        return menu;
    }
    //绑定
    JMenuItem bindMenu(DefaultMutableTreeNode node, TreeItem treeItem) {
        JMenuItem menu = new JMenuItem("绑定到当前项目");
        menu.addActionListener(e -> {
            if(new Confim(project,"确认","确定要绑定当前项目吗？").showAndGet()) {
                String name = project.getName();
                String path = treeItem.getPath()+"/auth.txt";
                ProjectAuthDto authDto = getAuthDto(path);
                if(null == authDto){
                    NotifyService.warning(project,"此应用还没有拥有者,绑定失败!");
                    return;
                }
                GrpcConfigService grpcConfig = GrpcConfigService.getInstance(project);
                //grpc.xml有，契约大仓有
                if(StringUtils.isNotBlank(grpcConfig.getIdlPath()) && StringUtils.isNotBlank(authDto.getProject())){
                    //绑定的是本项目，可以解绑
                    if(name.equals(authDto.getProject())){
                        PermissionDto permission = AuthService.getInstance(project).hasPermission(treeItem.getPath(), GitlabApiService.getCurrentUser());
                        //有权限操作
                        if(permission.isHasPermission()){
                            if(new Confim(project,"取消绑定","该文件夹已经绑定"+name+"项目,确定取消绑定吗？").showAndGet()) {
                                authDto.setProject("");
                                GitlabApiService.updateFile(project,path,JsonUtils.toString(authDto),"修改应用权限");
                                GrpcConfigService.getInstance(project).cancelPath();
                                NotifyService.info(project,"取消绑定成功");
                            }
                        }else{
                            NotifyService.warning(project,"当前用户无权限操作,请联系项目相关负责人进行操作！");
                        }
                    }else{
                        NotifyService.warning(project,"当前选中文件夹绑定过其他项目,当前项目绑定了其他文件夹,请相关项目负责人解绑后再进行绑定！");
                    }
                    return;
                }
                //grpc.xml有，契约大仓没有
                if(StringUtils.isNotBlank(grpcConfig.getIdlPath()) && StringUtils.isBlank(authDto.getProject())){
                    NotifyService.warning(project,"当前项目绑定过其他文件夹,请相关项目负责人解绑后再进行绑定！");
                    return;
                }
                //grpc.xml没有，契约大仓有
                if(StringUtils.isBlank(grpcConfig.getIdlPath()) && StringUtils.isNotBlank(authDto.getProject())){
                    NotifyService.warning(project,"当前选中文件夹绑定过其他项目,请相关项目负责人解绑后再进行绑定！");
                    return;
                }
                PermissionDto permission = AuthService.getInstance(project).hasPermission(treeItem.getPath(), GitlabApiService.getCurrentUser());
                if(permission.isHasPermission()){
                    authDto.setProject(name);
                    GitlabApiService.updateFile(project,path,JsonUtils.toString(authDto),"修改应用权限");
                    GrpcConfigService.getInstance(project).supportPath(treeItem.getPath());
                    refreshNode((DefaultMutableTreeNode)node.getParent());
                    NotifyService.info(project,"绑定成功");
                }else{
                    NotifyService.warning(project,"当前用户无权限操作,请联系项目相关负责人进行操作！");
                }
            }
        });
        return menu;
    }

    public ProjectAuthDto getAuthDto(String path){
        try {
            String content = GitlabApiService.getContent(project, path);
            if(StringUtils.isBlank(content)) {
                if(new Confim(project,"设置权限","此应用还没有拥有者，是否将当前用户设为拥有者").showAndGet()){
                    return AuthService.getInstance(project).initOwner(path);
                }
                return null;
            }
            return JsonUtils.toObject(content,ProjectAuthDto.class);
        }catch (Exception e){
            System.out.println("报错了");
        }
        return null;
    }

    java.util.List<JMenuItem> createNewMenu(TreeItem treeItem, DefaultMutableTreeNode node) {
        NodeType nodeType = treeItem.getNodeType();
        List<JMenuItem> items = new ArrayList<>();
        switch (nodeType) {
            case business:
                items.add(new CreateDirectoryMenu(project, "添加项目(应用)", treeItem, (newDirectoryName) -> {
                    String newPath = treeItem.getPath().concat("/").concat(newDirectoryName);
                    AuthService.getInstance(project).initOwner(newPath);
                    addChildTree(node,newPath);
                    //添加项目的时候，大概率没有添加版本的文件的诉求
//                    CreateProtoService.create(project, newPath.concat("/v1"), newDirectoryName + ".proto", (path) -> {
//                        addChildTree(node,newPath);
//                    });
                }));
                break;
            case project:
                items.add(new CreateDirectoryMenu(project,"添加版本", treeItem, (newDirectoryName) -> {
                    ProtoPath path = ProtoPath.newInstance(treeItem.getPath());
                    String projectName = path.getProject();
                    String fileName;
                    if (newDirectoryName.equals("v1")) {
                        fileName = projectName;
                    } else {
                        fileName = projectName + "_" + newDirectoryName;
                    }
                    String newPath = treeItem.getPath().concat("/").concat(newDirectoryName);
                    CreateProtoService.create(project, newPath, fileName + ".proto", (newDir) -> {
                        addChildTree(node,newDir);
                    });
                }));
                break;
            case version:
                items.add(new CreateProtoMenu().create(project, treeItem, node, (x) -> {
                    addChildBlob(node,treeItem.getPath()+"/"+x);
                }));
                items.add(new CreateDirectoryMenu(project,"添加模块", treeItem, (newModuleName) -> {
                    String newDir = treeItem.getPath().concat("/").concat(newModuleName);
                    CreateProtoService.create(project, newDir, newModuleName + ".proto", (newpath) -> {
                        addChildTree(node,newDir);
                    });
                }));
                break;
            case service:
                items.add(new CreateProtoMenu().create(project,treeItem, node, (x) -> {
                    addChildBlob(node,treeItem.getPath()+"/"+x);
                }));
                break;
            default:
                break;
        }
        return items;
    }

    void addChildBlob(DefaultMutableTreeNode parent, String path){
        ProtoPath protoPath = ProtoPath.newInstance(path);
        TreeItem newItem = new TreeItem();
        newItem.setType("blob");
        newItem.setFileFrom(FileFrom.LOCAL);
        newItem.setPath(path);
        newItem.setName(protoPath.getFileName());
        parent.add(new DefaultMutableTreeNode(newItem));
        defaultTree.updateUI();
    }


    DefaultMutableTreeNode addChildTree(DefaultMutableTreeNode parent, String path){
        TreeItem newItem = new TreeItem();
        newItem.setType("tree");
        newItem.setFileFrom(FileFrom.LOCAL);
        newItem.setPath(path);
        String name = path.substring(path.lastIndexOf("/")+1);
        newItem.setName(name);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(newItem);
        parent.add(node);
        openAll(node,getTreePath(node));
        defaultTree.updateUI();
        return node;
    }

    public void itemContentUpdate(String path){
        resetAction(path,GitLocalInfoType.update);
    }

    public void itemContentNotUpdate(String path){
        resetAction(path,GitLocalInfoType.none);
    }

    public void itemContentReload(String path){
        resetAction(path,GitLocalInfoType.none);
    }

    void resetAction(String path,GitLocalInfoType action){
        if(defaultTree == null)
            return;

        DefaultMutableTreeNode node = getNode(path);
        if(node==null)
            return;

        TreeItem treeItem = (TreeItem)node.getUserObject();
        treeItem.setAction(action);
        defaultTree.updateUI();
    }

    public void refreshStatus() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "title") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                refreshStatus(baseNode);
                defaultTree.updateUI();
            }
        });
    }

    public void refreshStatus(String path) {
        DefaultMutableTreeNode node = getNode(path);
        if (path == null)
            return;
        refreshStatus(node);
    }


    public void refreshStatus(DefaultMutableTreeNode node){
        TreeItem treeItem =(TreeItem) node.getUserObject();
        if(treeItem.getFileFrom().equals(FileFrom.LOCAL)) {
            if(treeItem.getType().equals("tree")){
                ApiResult<List<TreeItem>> list = GitlabApiService.getItems(project, treeItem.getPath());
                if(list.getResult().size()>0){
                    treeItem.setFileFrom(FileFrom.GITLAB);
                }
            }else{
                FileDetail file = GitlabApiService.getFile(project, treeItem.getPath());
                if (file != null) {
                    treeItem.setFileFrom(FileFrom.GITLAB);
                }
            }
        }

        if(treeItem.getAction() !=null && treeItem.getAction().equals(GitLocalInfoType.update)) {

            if (!FileChangeLog.getInstance(project).hasChange( VirtualFileService.get(Config.getInstance(project).protoBasePath() + treeItem.getPath()))) {
                treeItem.setAction(GitLocalInfoType.none);
            }
        }

        treeItem.setExistsLocal(FileService.exists(project, treeItem.getPath()));

        if(treeItem.getFileFrom().equals(FileFrom.LOCAL) && !treeItem.isExistsLocal()){
            node.removeFromParent();
            return;
        }

        node.setUserObject(treeItem);
        if(node.getChildCount()>0){
            for (int i = 0; i < node.getChildCount(); i++) {
                refreshStatus((DefaultMutableTreeNode)node.getChildAt(i));
            }
        }
    }




}
