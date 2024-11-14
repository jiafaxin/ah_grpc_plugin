package com.autohome.ah_grpc_plugin.models;

import org.apache.commons.lang3.StringUtils;

public class ProtoPath {

    public static ProtoPath newInstance(String path){
        int start = path.indexOf("autohome/rpc");
        if (start < 0) {
            return null;
        }
        return new ProtoPath(path);
    }

    private ProtoPath (String path) {
        int start = path.indexOf("autohome/rpc");
        if (start > 0) {
            path = path.substring(start);
        }

        if (path.indexOf(".proto") > 0) {
            setFileName(path.substring(path.lastIndexOf("/") + 1));
            path = path.substring(0,path.lastIndexOf("/"));
        }

        String[] pathList = path.split("/");
        if (pathList.length >= 3)
            setBusiness(pathList[2]);
        if (pathList.length >= 4)
            setProject(pathList[3]);
        if (pathList.length >= 5)
            setVersion(pathList[4]);
        if (pathList.length >= 6)
            setModule(pathList[5]);
    }

    String business;
    String project;
    String version;
    String module;
    String fileName;

    public boolean isProject() {
        return StringUtils.isNotBlank(project) && StringUtils.isBlank(version);
    }


    String basePath = "autohome/rpc";


    public String getProjectPath(){
        return basePath.concat("/").concat(business).concat("/").concat(project);
    }

    public String getDomian(){
        String base = business.concat("_").concat(project);
        return base;
    }

    public String realPath() {
        String path = basePath.concat("/" + business);
        if (StringUtils.isBlank(project))
            return path;

        path = path.concat("/" + project);
        if (StringUtils.isBlank(version))
            return path;

        path = path.concat("/" + version);

        if (StringUtils.isNotBlank(module)) {
            path += "/" + module;
        }

        if (StringUtils.isBlank(fileName)) {
            return path;
        }
        return path + "/" + fileName;
    }

    public String getBusiness() {
        return business;
    }

    public void setBusiness(String business) {
        this.business = business;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
