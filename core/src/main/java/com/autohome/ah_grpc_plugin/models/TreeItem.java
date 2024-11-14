package com.autohome.ah_grpc_plugin.models;

import com.autohome.ah_grpc_plugin.enums.FileFrom;
import com.autohome.ah_grpc_plugin.enums.GitLocalInfoType;
import com.autohome.ah_grpc_plugin.enums.NodeType;
import com.autohome.ah_grpc_plugin.services.GrpcConfigService;
import com.autohome.ah_grpc_plugin.utils.AutohomeIcons;
import com.intellij.icons.AllIcons;
import org.apache.xmlbeans.impl.xb.xsdschema.All;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TreeItem {
    String name;
    String path;
    String type;
    NodeType nodeType;
    FileFrom fileFrom = FileFrom.GITLAB;
    GitLocalInfoType action;
    boolean existsLocal = false;
    boolean isBindProject = false;


    public Icon getIcon() {
        if (getNodeType() == null)
            return AllIcons.Nodes.Folder;
        switch (getNodeType()) {
            case business:
            case project:
                if(isBindProject){
                    return AutohomeIcons.BindFolder;
                }
            case version:
            case service:
                if(existsLocal) {
                    return fileFrom.equals(FileFrom.LOCAL) ? AutohomeIcons.NewFolder : AllIcons.Modules.SourceRoot;
                }else {
                    return fileFrom.equals(FileFrom.LOCAL) ? AutohomeIcons.NewFolder : AllIcons.Nodes.Folder;
                }
            case proto:
                if (fileFrom.equals(FileFrom.LOCAL)) {
                    return AutohomeIcons.NewProto;
                }else {
                    if(existsLocal) {
                        return AutohomeIcons.ProtoFile;
                    }else{
                        return AutohomeIcons.OnlineProto;
                    }
                }
            default:
                return AllIcons.Modules.SourceRoot;
        }
    }

    public Color getColor(Color defaultColor) {
        if (getNodeType() == null)
            return defaultColor;
        if (fileFrom.equals(FileFrom.LOCAL)) {
            return defaultColor;
        }
        switch (getNodeType()) {
            case proto:
                if (action != null && action.equals(GitLocalInfoType.update)) {
                    return new Color(242, 101, 34);
                }
            default:
                return defaultColor;
        }
    }

    public NodeType getNodeType() {
        if(getType().equals("blob"))
            return NodeType.proto;
        int l = getPath().split("/").length;
        switch (l){
            case 1:
            case 2:
                return NodeType.root;
            case 3:
                return NodeType.business;
            case 4:
                return NodeType.project;
            case 5:
                return NodeType.version;
            case 6:
                return NodeType.service;
        }
        return nodeType;
    }

    public FileFrom getFileFrom() {
        return fileFrom;
    }

    public void setFileFrom(FileFrom fileFrom) {
        this.fileFrom = fileFrom;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String toString() {
        return name;
    }

    public GitLocalInfoType getAction() {
        return action;
    }

    public void setAction(GitLocalInfoType action) {
        this.action = action;
    }

    public boolean isExistsLocal() {
        return existsLocal;
    }

    public void setExistsLocal(boolean existsLocal) {
        this.existsLocal = existsLocal;
    }

    public boolean isBindProject() {
        return isBindProject;
    }

    public void setBindProject(boolean bindProject) {
        isBindProject = bindProject;
    }
}
