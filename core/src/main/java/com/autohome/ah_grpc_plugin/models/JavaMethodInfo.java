package com.autohome.ah_grpc_plugin.models;

public class JavaMethodInfo {
    String content;
    String requestPackage;
    String responsePackage;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRequestPackage() {
        return requestPackage;
    }

    public void setRequestPackage(String requestPackage) {
        this.requestPackage = requestPackage;
    }

    public String getResponsePackage() {
        return responsePackage;
    }

    public void setResponsePackage(String responsePackage) {
        this.responsePackage = responsePackage;
    }
}
