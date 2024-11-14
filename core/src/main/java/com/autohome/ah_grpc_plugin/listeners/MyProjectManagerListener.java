package com.autohome.ah_grpc_plugin.listeners;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.factory.BufToolWindow;
import com.autohome.ah_grpc_plugin.factory.MainFactory;
import com.autohome.ah_grpc_plugin.providers.services.ImplProtoService;
import com.autohome.ah_grpc_plugin.providers.services.ProtoImplService;
import com.autohome.ah_grpc_plugin.services.FileChangeLog;
import com.autohome.ah_grpc_plugin.services.GitLocalService;
import com.autohome.ah_grpc_plugin.services.GrpcConfigService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;


/**
 * 不再在项目启动的时候扫描项目了
 */
public class MyProjectManagerListener implements ProjectManagerListener {

//    public void projectOpened(@NotNull Project project) {
//
////        DumbService dumbService = DumbService.getInstance(project);
////        if(dumbService.isDumb()){
////            dumbService.runWhenSmart(()->{
////                initProtoAndImpl(project);
////            });
////        }else{
////            initProtoAndImpl(project);
////        }
//    }
//
//    void initProtoAndImpl(Project project){
//        ProtoImplService.getInstance(project).init();
//        ImplProtoService.getInstance(project).init();
//    }

}
