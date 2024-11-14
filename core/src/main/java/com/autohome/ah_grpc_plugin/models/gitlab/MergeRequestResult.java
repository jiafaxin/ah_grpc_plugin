package com.autohome.ah_grpc_plugin.models.gitlab;

public class MergeRequestResult {
    Integer id;

    Integer iid;

    String merge_status;

    public String getMerge_status() {
        return merge_status;
    }

    public void setMerge_status(String merge_status) {
        this.merge_status = merge_status;
    }

    public Integer getIid() {
        return iid;
    }

    public void setIid(Integer iid) {
        this.iid = iid;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
