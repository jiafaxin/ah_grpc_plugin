package com.autohome.ah_grpc_plugin.listeners;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.enums.NodeType;
import com.autohome.ah_grpc_plugin.platform.PlatformService;
import com.autohome.ah_grpc_plugin.providers.services.ProtoImplService;
import com.autohome.ah_grpc_plugin.services.*;
import com.autohome.ah_grpc_plugin.utils.PathUtils;
import com.autohome.ah_grpc_plugin.utils.ProjectFileUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.NoAccessDuringPsiEvents;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

//拦截project下的文件的创建、修改、删除等操作
public class MyBulkFileListener implements BulkFileListener {

    @Override
    public void before(@NotNull List<? extends @NotNull VFileEvent> events) {

        if (NoAccessDuringPsiEvents.isInsideEventProcessing())
            return;

        if (events == null || events.size() == 0)
            return;

        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file == null)
                continue;

            for (Project fileProject : ProjectFileUtil.getFileProjects(file)) {
                if(!GrpcConfigService.getInstance(fileProject).getSupportGrpc()){
                    continue;
                }
                PlatformService platformService = Config.getInstance(fileProject).platformConfig().getPlatform();
                if ((event instanceof VFileDeleteEvent) && platformService.isImplFile(event.getFile())) {
                    ApplicationManager.getApplication().runReadAction(() -> {
                        PsiFile psiFile = PsiManager.getInstance(fileProject).findFile(file);
                        ProtoImplService.getInstance(fileProject).deleteImplClass(psiFile);
                    });
                }
            }
        }
    }


    void implFile(Project project, VFileEvent event, VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

        ApplicationManager.getApplication().runReadAction(() -> {
            if (event instanceof VFileContentChangeEvent || event instanceof VFileMoveEvent) {
                ProtoImplService.getInstance(project).initImplClass(psiFile);
            } else if (event instanceof VFileDeleteEvent) {
                ProtoImplService.getInstance(project).deleteImplClass(psiFile);
            }
        });
    }


    @Override
    public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        if (NoAccessDuringPsiEvents.isInsideEventProcessing())
            return;

        if (events == null || events.size() == 0)
            return;

        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file == null)
                continue;

            for (Project fileProject : ProjectFileUtil.getFileProjects(file)) {

                if(!GrpcConfigService.getInstance(fileProject).getSupportGrpc()){
                    continue;
                }
                PlatformService platformService =  Config.getInstance(fileProject).platformConfig().getPlatform();
                if (platformService.isImplFile(event.getFile())) {
                    if (event instanceof VFileContentChangeEvent || event instanceof VFileMoveEvent) {
                        ApplicationManager.getApplication().runWriteAction(() -> {
                            PsiFile psiFile = PsiManager.getInstance(fileProject).findFile(file);
                            ProtoImplService.getInstance(fileProject).initImplClass(psiFile);
                        });
                    }
                } else {
                    doPbFile(fileProject, event);
                }
            }
        }
    }


    void doPbFile(Project project, VFileEvent event) {
        VirtualFile file = event.getFile();
        String protoPath = PathUtils.getProtoPath(project, file.getPath());
        if (StringUtils.isBlank(protoPath))
            return;

        NodeType nodeType = PathUtils.getNodeType(protoPath);

        if (nodeType.equals(NodeType.error)) {
            NotifyService.warning(project, "您创建的文件格式不正确，无法加入汽车之家契约大仓，请通过”右键 > Autohome GRPC 插件“菜单操作");
            return;
        }

        if (event instanceof VFileCreateEvent) {
            if (file.isDirectory()) {
                if (PathUtils.getNodeType(protoPath) == NodeType.project
                        || PathUtils.getNodeType(protoPath) == NodeType.version
                        || PathUtils.getNodeType(protoPath) == NodeType.service
                ) {
                    GitLocalService.getInstance(project).createDirectory(protoPath);
                } else {
                    NotifyService.error(project, "src/main/proto文件夹下，除了创建应用、版本、模块外，创建其他文件夹都是无效的！");
                }
            } else {
                GitLocalService.getInstance(project).create( protoPath);
                //创建的文件，会把proto的模板填入文件
            }
        } else if (event instanceof VFileDeleteEvent) {
            GitLocalService.getInstance(project).delete(protoPath);
        } else if (event instanceof VFileContentChangeEvent) {
            if (FileChangeLog.getInstance(project).hasChange(file)) {
                GitLocalService.getInstance(project).update(protoPath);
            }
        } else if (event instanceof VFilePropertyChangeEvent) {
            VFilePropertyChangeEvent event1 = (VFilePropertyChangeEvent) event;
            if (event1.getNewValue().toString().equals(event1.getOldValue().toString())) {
                //新建的文件，有时候也会有rename的事件，同名不处理
                return;
            }
            String propertyName = event1.getPropertyName();
            if (propertyName.equals("name")) {
                //重命名
                new ProtoFileService(project, file).rename(project, event1);
                NotifyService.warning(project, "重命名文件对于大仓来说相当于复制文件，不会删除原文件，防止原服务不可用");
            }
        } else if (event instanceof VFileMoveEvent) {
            VFileMoveEvent moveEvent = (VFileMoveEvent) event;
            new ProtoFileService(project, file).move(project, moveEvent);
        }
    }
}
