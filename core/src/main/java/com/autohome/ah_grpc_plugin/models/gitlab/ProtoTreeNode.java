package com.autohome.ah_grpc_plugin.models.gitlab;

import lombok.Data;
import java.util.List;


public class ProtoTreeNode {
    String name;
    String path;
    String type;
    List<ProtoTreeNode> childs;

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

    public List<ProtoTreeNode> getChilds() {
        return childs;
    }

    public void setChilds(List<ProtoTreeNode> childs) {
        this.childs = childs;
    }
}
