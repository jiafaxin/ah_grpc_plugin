package com.autohome.ah_grpc_plugin.models.gitlab;

public class NewFileParam {
    String branch;
    String content;
    String commit_message;

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCommit_message() {
        return commit_message;
    }

    public void setCommit_message(String commit_message) {
        this.commit_message = commit_message;
    }
}
