package com.autohome.ah_grpc_plugin.utils;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.enums.NodeType;
import com.intellij.openapi.project.Project;


/**
 * path的标准路径应该是：autohome/rpc/project/version/module/x.proto
 */
public class PathUtils {


    public static int isProtoPath(Project project,String path){
        if(path==null)
            return -1;
        if(Config.getInstance(project).protoBasePath()==null)
            return -1;
        if(path.indexOf("/proto/google/")>0) {
            return path.indexOf(Config.getInstance(project).protoBasePath() + "google/");
        }
        return path.indexOf(Config.getInstance(project).protoBasePath() + "autohome/");
    }

    public static String getProtoPath(Project project, String path){
        int isProtoPath = isProtoPath(project,path);
        if(isProtoPath<0)
            return null;
        if(path.length()<Config.getInstance(project).protoBasePath().length())
            return path;
        return path.substring(Config.getInstance(project).protoBasePath().length());
    }

    public static String[] getProtoFilePath(String path){
        String[] result = new String[2];
        result[0] = path.substring(0,path.lastIndexOf("/"));
        result[1] = path.substring(path.lastIndexOf("/")+1);
        return result;
    }

    public static NodeType getNodeType(String path){
        if(path.indexOf(".proto")>0)
            return NodeType.proto;
        switch (path.split("/").length){
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
