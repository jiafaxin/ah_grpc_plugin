package com.autohome.ah_grpc_plugin.models;

import com.intellij.openapi.vfs.VirtualFile;

public class FileChangeInfo {

    String path;
    String sha256;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }
}
