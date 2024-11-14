package com.autohome.ah_grpc_plugin.platform;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;

public interface PlatformConfig {

    static PlatformConfig getInstance(Project project) {
        return project.getService(PlatformConfig.class);
    }

    SupportGRPC getSuppprtGRPC();

    PlatformService getPlatform();

    boolean isMultiModule(Project project);

    boolean hasGrpcModule(Project project);
}
