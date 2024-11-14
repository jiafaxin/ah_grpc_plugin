package com.autohome.ah_grpc_plugin.models;

import java.util.List;

public class PermissionDto {
    boolean hasPermission;
    List<String> owners;

    public boolean isHasPermission() {
        return hasPermission;
    }

    public void setHasPermission(boolean hasPermission) {
        this.hasPermission = hasPermission;
    }

    public List<String> getOwners() {
        return owners;
    }

    public void setOwners(List<String> owners) {
        this.owners = owners;
    }
}
