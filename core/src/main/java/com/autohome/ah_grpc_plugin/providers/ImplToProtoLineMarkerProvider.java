package com.autohome.ah_grpc_plugin.providers;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.enums.ResultCode;
import com.autohome.ah_grpc_plugin.models.MethodResult;
import com.autohome.ah_grpc_plugin.models.ProtoLineMarkerInfo;
import com.autohome.ah_grpc_plugin.platform.PlatformService;
import com.autohome.ah_grpc_plugin.providers.services.ImplProtoService;
import com.autohome.ah_grpc_plugin.services.GrpcConfigService;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.protobuf.lang.psi.PbServiceMethod;
import com.intellij.protobuf.lang.psi.impl.PbServiceDefinitionImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.NullableFunction;
import com.intellij.util.PsiNavigateUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 实现服务端代码
 */
public class ImplToProtoLineMarkerProvider implements LineMarkerProvider {

    PlatformService pi;

    Project project;

    ImplProtoService service;

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        project = element.getProject();

        if(!GrpcConfigService.getInstance(project).getSupportGrpc()){
            return null;
        }


        pi = Config.getInstance(project).platformConfig().getPlatform();
        service = ImplProtoService.getInstance(project);
        return implToProto(element);
    }

    public LineMarkerInfo<?> implToProto(@NotNull PsiElement element) {


        ProtoLineMarkerInfo info = Config.getInstance(project).getPlatformConfig().getPlatform().getProtoLineMarkerInfo(element);
        if(info == null)
            return null;



        //不在此处设置跳转地址
        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder
                .create(AllIcons.Nodes.Property)
                .setTooltipText("定位到契约文件")
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(new ArrayList<>());

        return builder.createLineMarkerInfo(info.getLineOn(), (e1, elt) -> {
            openProto(element.getProject(), info.getServicePath(), info.getMethodName());
        });
    }


    //打开proto
    public MethodResult openProto(Project project, String protoServicePath, String methodName) {
        if (!service.contains(protoServicePath)) {
            return MethodResult.fail(ResultCode.PROTO_NOT_EXISTS);
        }

        //PSI 不可信，需要重建
        String[] pathAndName = service.get(protoServicePath).split("@@");
        File file = new File(pathAndName[0]);
        if(!file.exists()){
            return MethodResult.fail(ResultCode.PROTO_NOT_EXISTS);
        }
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);

        PsiFile simpleFile = PsiManager.getInstance(project).findFile(virtualFile);
        List<PbServiceDefinitionImpl> services = PsiTreeUtil.getChildrenOfTypeAsList(simpleFile, PbServiceDefinitionImpl.class);
        for (PbServiceDefinitionImpl service : services) {
            if (service.getName().equals(pathAndName[1])) {
                if(StringUtils.isBlank(methodName)) {
                    PsiNavigateUtil.navigate(service);
                    break;
                }else{
                    boolean getMethod = false;
                    for (PbServiceMethod method : service.getBody().getServiceMethodList()) {
                        if(pi.methodName(method.getName()).equals(methodName)){
                            PsiNavigateUtil.navigate(method);
                            getMethod = true;
                            break;
                        }
                    }
                    if(getMethod)
                        break;
                }
            }
        }
        return MethodResult.success();
    }
}
