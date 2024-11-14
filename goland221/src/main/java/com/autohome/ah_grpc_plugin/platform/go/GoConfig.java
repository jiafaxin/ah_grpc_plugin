package com.autohome.ah_grpc_plugin.platform.go;

import com.autohome.ah_grpc_plugin.platform.PlatformConfig;
import com.autohome.ah_grpc_plugin.platform.PlatformService;
import com.autohome.ah_grpc_plugin.platform.SupportGRPC;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

public final class GoConfig implements PlatformConfig {

    Project project;

    public GoConfig(Project project) {
        this.project = project;
    }


    @Override
    public SupportGRPC getSuppprtGRPC() {

        return new GoSupportGRPC();
    }

    @Override
    public PlatformService getPlatform() {
        return new GoPlatformService();
    }

    @Override
    public boolean isMultiModule(Project project) {
        return false;
    }

    @Override
    public boolean hasGrpcModule(Project project) {
        return false;
    }
}
