package com.autohome.ah_grpc_plugin.models;

import com.autohome.ah_grpc_plugin.enums.FileFrom;
import com.autohome.ah_grpc_plugin.enums.GitLocalInfoType;
import com.autohome.ah_grpc_plugin.enums.NodeType;
import com.autohome.ah_grpc_plugin.utils.AutohomeIcons;
import com.intellij.icons.AllIcons;

import javax.swing.*;
import java.util.List;

public class GitActionTreeItem {
    GitLocalInfoType action;
    String path;
    List<GitActionTreeItem> children;

    public GitLocalInfoType getAction() {
        return action;
    }

    public void setAction(GitLocalInfoType action) {
        this.action = action;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    public List<GitActionTreeItem> getChildren() {
        return children;
    }

    public void setChildren(List<GitActionTreeItem> children) {
        this.children = children;
    }

    public String toString(){
        if(path.equals("autohome/rpc"))
            return "Autohome grpc protos";
        return path.substring(path.lastIndexOf("/")+1);
    }


    public Icon getIcon(){
        if(getAction()==null)
            return AllIcons.Nodes.Folder;
        switch (getNodeType()){
            case business:
            case project:
            case version:
            case service:
                return getAction().equals(GitLocalInfoType.create)? AutohomeIcons.NewFolder : AllIcons.Nodes.Folder;
            case proto:
                return getAction().equals(GitLocalInfoType.create)? AutohomeIcons.NewProto : AutohomeIcons.ProtoFile;
            default:
                return AllIcons.Modules.SourceRoot;
        }
    }


    public NodeType getNodeType() {
        if(getPath().indexOf(".proto")>0)
            return  NodeType.proto;
        int l = getPath().split("/").length;
        switch (l) {
            case 1:
            case 2:
                return NodeType.root;
            case 3:
                return NodeType.business;
            case 4:
                return NodeType.project;
            case 5:
                return NodeType.version;
            default:
                return NodeType.service;
        }
    }
}
