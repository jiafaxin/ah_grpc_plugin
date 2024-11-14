package com.autohome.ah_grpc_plugin.platform.go;

import com.autohome.ah_grpc_plugin.platform.SupportGRPC;
import com.autohome.ah_grpc_plugin.services.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectNotificationAware;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;

import java.util.Set;

public class GoSupportGRPC implements SupportGRPC {

    Project project;


    @Override
    public boolean support(Project project) {
        return true;
    }

    /**
     * go 让项目支持grpc
     * @param project
     * @param currPath
     */
    @Override
    public boolean support(Project project, AnActionEvent e, String currPath) {
        if(!currPath.equals(project.getBasePath())){
            NotifyService.error(project,"请在根路径下让项目支持grpc");
            return false;
        }
        ApplicationManager.getApplication().runWriteAction(()->{
            String path = "proto/autohome/rpc";
            //创建文件夹
            initProtoDir(project,path);
            //下载grpc相关的proto
            CreateProtoService.downloadApiProto(project);

            String grpcModulePath = "/";
            GrpcConfigService.getInstance(project).support(grpcModulePath);
        });
        return true;
    }



    void initProtoDir(Project project,String path) {
        PsiService.createAndGetDirectory(project,path);
    }


    /**
     *
     * @param project
     * @param e
     * @param currPath
     * @param isConsumer
     * @return
     */
    @Override
    public boolean supportGrpc(Project project, AnActionEvent e, String currPath, boolean isConsumer) {
        if(!currPath.equals(project.getBasePath())){
            NotifyService.error(project,"请在根路径下让项目支持grpc");
            return false;
        }
        ApplicationManager.getApplication().runWriteAction(()->{
            if(!isConsumer){
                String path = "proto/autohome/rpc";
                //创建文件夹
                initProtoDir(project,path);
                //下载grpc相关的proto
                CreateProtoService.downloadApiProto(project);
            }
            //创建grpc.xml
            String grpcModulePath = "/";
            GrpcConfigService.getInstance(project).support(grpcModulePath);
        });
        return true;
    }
}
