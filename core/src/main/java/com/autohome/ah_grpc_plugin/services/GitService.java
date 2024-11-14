package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.models.gitlab.ProtoTreeNode;
import com.autohome.ah_grpc_plugin.utils.HttpClient;
import com.autohome.ah_grpc_plugin.utils.JsonUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.squareup.okhttp.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service(Service.Level.APP)
public final class GitService {

    ProtoTreeNode root;

    public static GitService getInstance() {
        GitService gitService = ApplicationManager.getApplication().getService(GitService.class);
        return gitService;
    }

    public ProtoTreeNode getTree(){
        if(root == null){
            loadAllNodes();
        }
        return root;
    }

    public ProtoTreeNode reloadTree(String path) {
        loadAllNodes();
        return getByPath(path, root);
    }

    ProtoTreeNode getByPath(String path,ProtoTreeNode node) {
        if (node.getPath().equals(path))
            return node;
        if (!path.startsWith(node.getPath()))
            return null;
        if (node.getChilds() == null || node.getChilds().size() == 0)
            return null;
        for (ProtoTreeNode child : node.getChilds()) {
            if (path.startsWith(child.getPath())) {
                return getByPath(path, child);
            }
        }
        return null;
    }

    boolean loadAllNodes() {
        String url = "https://git-ops.corpautohome.com/api/v4/projects/107/repository/tree?path=autohome/rpc&recursive=true&per_page=100&path=autohome/rpc";
        Response response = HttpClient.get(url + "&page=1");

        if (!response.isSuccessful())
            return false;
        Vector<ProtoTreeNode> nodes = new Vector<>();
        try {
            String value = response.body().string();
            int pageCount = Integer.parseInt(response.header("X-Total-Pages"));
            nodes.addAll(JsonUtils.toObjectList(value, ProtoTreeNode.class));
            AtomicInteger errorCount = new AtomicInteger(0);
            if (pageCount > 1) {
                List<CompletableFuture> tasks = new ArrayList<>();
                for (int i = 2; i <= pageCount; i++) {
                    int finalI = i;
                    tasks.add(CompletableFuture.runAsync(() -> {
                        Response response2 = HttpClient.get(url + "&page=" + finalI);
                        if (response2.isSuccessful()) {
                            try {
                                nodes.addAll(JsonUtils.toObjectList(response2.body().string(), ProtoTreeNode.class));
                            } catch (IOException e) {
                                errorCount.incrementAndGet();
                            }
                        }
                    }));
                }
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).join();
                if (errorCount.get() > 0) {
                    return false;
                }
            }
        } catch (IOException e) {
            return false;
        }
        convertNodes(nodes);
        return true;
    }



    void convertNodes(List<ProtoTreeNode> nodes) {
        //排除非proto文件的其他文件
        nodes.removeIf(x -> x.getType().equals("blob") && !x.getName().endsWith(".proto"));
        root = new ProtoTreeNode() {{
            setChilds(new ArrayList<>());
            setName("autohome rpc");
            setPath("autohome/rpc");
            setType("tree");
        }};
        convertItem(root, nodes);
    }

    void convertItem(ProtoTreeNode parent, List<ProtoTreeNode> nodes) {
        String parentPath = parent.getPath();
        List<ProtoTreeNode> list = nodes.stream().filter(x -> {
            if (!x.getPath().startsWith(parentPath))
                return false;
            if (x.getPath().length() <= (parentPath.length() + 1))
                return false;
            return x.getPath().substring(parentPath.length() + 1).indexOf("/") < 0;
        }).collect(Collectors.toList());

        for (ProtoTreeNode gitTreeNode : list) {
            if(gitTreeNode.getType().equals("tree")){
                convertItem(gitTreeNode,nodes);
            }
        }
        parent.setChilds(list);
    }


//    public CompletableFuture<Integer> push(GitPushList pushList){
//        //创建分支
//        return GitlabApiService.createBranches(project).thenCompose(branch->{
//            String branchName = branch.getResult().getName();
//            pushList.setBranch(branchName);
//            //提交变更到分支
//            return GitlabApiService.commit(project,pushList).thenCompose(commitResult->{
//                //创建合并请求
//                return GitlabApiService.mergeRequests(project,branchName).thenCompose(mergeResult->{
//                    //合并请求
//                    return GitlabApiService.merge(project,mergeResult.getResult().getIid()).thenApply(marge->{
//                        return marge.getCode();
//                    });
//                });
//            });
//        });
//    }

}
