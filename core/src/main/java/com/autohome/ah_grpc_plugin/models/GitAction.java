package com.autohome.ah_grpc_plugin.models;

import com.autohome.ah_grpc_plugin.enums.GitLocalInfoType;

public class GitAction {
    GitLocalInfoType action = GitLocalInfoType.none;
    String file_path;
    String content;

    Boolean isDirectory = false;


    public String getFileName() {
        String[] fps = file_path.split("/");
        String fileName = fps[fps.length - 1];
        return fileName;
    }

    public Boolean getDirectory() {
        return isDirectory;
    }

    public void setDirectory(Boolean directory) {
        isDirectory = directory;
    }

    public GitLocalInfoType getAction() {
        return action;
    }

    public void setAction(GitLocalInfoType action) {
        this.action = action;
    }

    public String getFile_path() {
        return file_path;
    }

    public void setFile_path(String file_path) {
        this.file_path = file_path;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
