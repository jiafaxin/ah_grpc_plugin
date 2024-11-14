package com.autohome.ah_grpc_plugin.activities;

import com.autohome.ah_grpc_plugin.providers.services.ImplProtoService;
import com.autohome.ah_grpc_plugin.providers.services.ProtoImplService;
import com.autohome.ah_grpc_plugin.services.BufExeService;
import com.autohome.ah_grpc_plugin.services.GrpcConfigService;
import com.autohome.ah_grpc_plugin.services.UpdateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class InitActivity implements StartupActivity.RequiredForSmartMode  {

    @Override
    public void runActivity(@NotNull Project project) {
//
//
//        if (!GrpcConfigService.getInstance(project).getSupportGrpc()) {
//            return;
//        }
//
//        UpdateService.show(project);
//
//        ProgressManager.getInstance().run(new Task.Backgroundable(project, "更新buf") {
//            @Override
//            public void run(@NotNull ProgressIndicator indicator) {
//                BufExeService.downExe(project, null);
//            }
//        });
    }


}
