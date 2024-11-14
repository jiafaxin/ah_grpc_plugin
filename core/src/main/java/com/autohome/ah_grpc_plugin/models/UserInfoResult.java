package com.autohome.ah_grpc_plugin.models;

import java.util.List;

public class UserInfoResult {
    Integer status;
    List<UserInfoData> data;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<UserInfoData> getData() {
        return data;
    }

    public void setData(List<UserInfoData> data) {
        this.data = data;
    }
}
