package com.autohome.ah_grpc_plugin.models;

import com.autohome.ah_grpc_plugin.utils.AutohomeIcons;
import com.intellij.icons.AllIcons;

import javax.swing.*;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ChangeTreeItem {

    public ChangeTreeItem(String name,String path){
        setName(name);
        setPath(path);
    }

    public ChangeTreeItem(String filePath) {
        setFilePath(filePath);
        String[] pps = filePath.split("/");
        setName(pps[pps.length-1]);
        setPath(String.join("/", Arrays.stream(pps).limit(pps.length - 1).collect(Collectors.toList())));
    }

    String path;
    String filePath;
    String name;

    public Icon getIcon(){
        if(name.equals("所有变更")){
            return AllIcons.Actions.ListChanges;
        }else if(name.indexOf(".proto")>0){
            return AutohomeIcons.ProtoFile;
        }else{
            return AllIcons.Nodes.Folder;
        }
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String toString(){
        return getName();
    }
}
