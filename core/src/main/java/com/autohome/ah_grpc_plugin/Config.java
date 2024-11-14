package com.autohome.ah_grpc_plugin;

import com.autohome.ah_grpc_plugin.platform.PlatformConfig;
import com.autohome.ah_grpc_plugin.services.GrpcConfigService;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;

import java.io.File;

/**
 * crated by shicuining 2023/1/2
 */

@Service(Service.Level.PROJECT)
public final class Config {

    public final static String toowindowId = "AUTOHOME gRPC 契约大仓";
    public final static String gitProjectId = "107";

    //携带了登录信息
    public final static String gitBasePath = "https://idl:xEhr9yMcCsCzqENYiJ6X@git-ops.corpautohome.com/microservice/idl.git";

    String protoBaseFolder;

    Project project;

    String language;

    PlatformConfig platformConfig;

    String protoBasePath = null;

    Boolean isGo;

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public static Config getInstance(Project project) {
        Config service = project.getService(Config.class);
        if (service.getProject() != null)
            return service;
        service.setProject(project);
        service.initLanguage();
        return service;
    }

    public boolean isGo(){
        if(isGo==null){
            isGo = getLanguage().equals("go");
        }
        return isGo;
    }

    /**
     *
     * @return E://workspace/demo/grpc
     */
    public String grpcPath() {
        String path = project.getBasePath();
        switch (getLanguage()) {
            case "java":
                path += GrpcConfigService.getInstance(project).getGrpcModulePath();
                break;
        }
        return path;
    }

    /**
     *
     * @return java:/grpc/src/main/proto/  go:/proto/
     */
    public String getProtoBaseFolder() {
        if (protoBaseFolder == null) {
            String path = "";
            switch (getLanguage()) {
                case "go":
                    path = ("/proto/");
                    break;
                case "java":
                    path = ("src/main/proto/");
                    path = GrpcConfigService.getInstance(project).getGrpcModulePath() + path;
                    break;
            }
            protoBaseFolder = path;
        }
        return protoBaseFolder;
    }


    /**
     *
     * @return java:E://workspace/demo/grpc/src/main/proto/  go: E://workspace/demo/proto/
     */
    public String protoBasePath() {
        if (protoBasePath == null) {
            String path = project.getBasePath();
            if (path == null)
                return null;

            String baseFolder = getProtoBaseFolder();
            if (baseFolder == null)
                return null;
            protoBasePath = path.concat(baseFolder);
        }
        return protoBasePath;
    }

    public PlatformConfig platformConfig() {
        setPlatformConfig(project.getService(PlatformConfig.class));
//        if (getPlatformConfig() == null) {
//            String className = "com.autohome.ah_grpc_plugin.platform.java.JavaConfig";
//            switch (getLanguage()) {
//                case "go":
//                    className = "com.autohome.ah_grpc_plugin.platform.go.GoConfig";
//            }
//            try {
//                Class clazz = Class.forName(className);
//                setPlatformConfig((PlatformConfig) project.getService(clazz));
//            } catch (ClassNotFoundException e) {
//                throw new RuntimeException(e);
//            }
//        }
        return getPlatformConfig();
    }

    public void initLanguage() {
        //默认java
        String platform = "java";
        String goFile = project.getBasePath() + "/go.mod";
        File file = new File(goFile);
        if (file.exists()) {
            platform = "go";
        }
        setLanguage(platform);
    }


    public PlatformConfig getPlatformConfig() {
        return project.getService(PlatformConfig.class);
    }

    public void setPlatformConfig(PlatformConfig platformConfig) {
        this.platformConfig = platformConfig;
    }


    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
