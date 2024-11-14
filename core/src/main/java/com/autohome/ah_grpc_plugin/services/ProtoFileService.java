package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.pbe.Languages;
import com.autohome.ah_grpc_plugin.ui.MainToolwindow;
import com.autohome.ah_grpc_plugin.utils.PathUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.intellij.protobuf.lang.PbLanguage;
import com.intellij.protobuf.lang.psi.PbFile;
import com.intellij.protobuf.lang.psi.PbOptionExpression;
import com.intellij.protobuf.lang.psi.impl.PbFileImpl;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ProtoFileService {


    PbFileImpl pbFile;

    public ProtoFileService(Project project, VirtualFile virtualFile){
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        pbFile = (PbFileImpl) psiFile;
    }

    /**
     * 重命名Proto文件
     * @param event
     */
    public void rename(Project project, VFilePropertyChangeEvent event) {
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                String newFileName = event.getNewValue().toString();
                newFileName = newFileName.substring(0, newFileName.indexOf("."));
                for (PbOptionExpression option : pbFile.getOptions()) {
                    String optionText = "";
                    switch (option.getOptionName().getText()) {
                        case "java_outer_classname":
                            optionText = "option java_outer_classname = \"" + newFileName + "Proto\";";
                            break;
                        default:
                            continue;
                    }
                    PbFile file = (PbFile) PsiFileFactory.getInstance(project).createFileFromText(Languages.PB_LANGUAGE, optionText);
                    option.replace(file.getOptions().get(0));
                }

                event.getFile().refresh(true,false);
                GitLocalService.getInstance(project).create(PathUtils.getProtoPath(project,event.getNewPath()));
            });
        });
    }

    //移动文件
    public void move(Project project, VFileMoveEvent event) {
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                String newPath = PathUtils.getProtoPath(project,event.getNewPath());
                newPath = newPath.substring(0, newPath.lastIndexOf("/"));
                String basePackage = ProtoContentService.basePackage(newPath);

                for (PbOptionExpression option : pbFile.getOptions()) {
                    String optionText = "";
                    switch (option.getOptionName().getText()) {
                        case "java_package":
                            optionText = "option java_package = \"" + basePackage + "\";";
                            break;
                        case "go_package":
                            optionText = "option go_package = \"" + ProtoContentService.goPackage(newPath) + "\";";
                            break;
                        case "csharp_namespace":
                            optionText = "option csharp_namespace=\"" + basePackage + "\";";
                            break;
                        default:
                            continue;
                    }
                    PbFile file = (PbFile) PsiFileFactory.getInstance(project).createFileFromText(Languages.PB_LANGUAGE, optionText);
                    option.replace(file.getOptions().get(0));
                }
                PbFile file = (PbFile) PsiFileFactory.getInstance(project).createFileFromText(Languages.PB_LANGUAGE, "package "+basePackage+";");
                pbFile.getPackageStatement().replace(file.getPackageStatement());
                event.getFile().refresh(true,false);
                GitLocalService.getInstance(project).create(PathUtils.getProtoPath(project,event.getNewPath()));
            });
        });
    }

    public static void reload(Project project, String path) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                String content = GitlabApiService.getContent( project,path);
                if (StringUtils.isBlank(content)) {
                    NotifyService.error(project, "拉取失败:未拉取到文件");
                    return;
                }
                VirtualFile file = VirtualFileService.get(Config.getInstance(project).protoBasePath() + path);
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
                FileDocumentManager.getInstance().saveDocument(document);
                file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                file.refresh(true, false);
                NotifyService.info(project, "更新完成！");
            } catch (IOException ex) {
                NotifyService.error(project, "更新失败，请重试！");
            }
        });
        GitLocalService.getInstance(project).removeItem( path);
        MainToolwindow.getInstance(project).itemContentReload(path);
    }



}
