package com.autohome.ah_grpc_plugin.models;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class GitPushList {
    String branch;
    String commit_message;
    List<GitAction> actions;

    public String getBranch() {
        if(StringUtils.isBlank(branch)) {
            return "master";
        }
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCommit_message() {
        return commit_message;
    }

    public void setCommit_message(String commit_message) {
        this.commit_message = commit_message;
    }

    public List<GitAction> getActions() {
        return actions;
    }

    public void setActions(List<GitAction> actions) {
        this.actions = actions;
    }
}
