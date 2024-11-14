package com.autohome.ah_grpc_plugin.providers;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.enums.ResultCode;
import com.autohome.ah_grpc_plugin.models.MethodResult;
import com.autohome.ah_grpc_plugin.platform.PlatformService;
import com.autohome.ah_grpc_plugin.providers.services.ProtoImplService;
import com.autohome.ah_grpc_plugin.services.GrpcConfigService;
import com.autohome.ah_grpc_plugin.services.NotifyService;
import com.autohome.ah_grpc_plugin.utils.ProtoPsiUtils;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.protobuf.lang.psi.ProtoKeywordTokenType;
import com.intellij.protobuf.lang.psi.ProtoLeafElement;
import com.intellij.protobuf.lang.psi.impl.PbServiceBodyImpl;
import com.intellij.protobuf.lang.psi.impl.PbServiceDefinitionImpl;
import com.intellij.protobuf.lang.psi.impl.PbServiceMethodImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.PsiNavigateUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.util.ArrayList;

/**
 * 实现服务端代码
 */
public class ProtoImplementLineMarkerProvider implements LineMarkerProvider {

    PlatformService platformService;

    ProtoImplService protoImplService;

    Project project;

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        project = element.getProject();

        if(!GrpcConfigService.getInstance(project).getSupportGrpc()){
            return null;
        }

        platformService = Config.getInstance(project).platformConfig().getPlatform();
        protoImplService = ProtoImplService.getInstance(project);
        return protoToImpl(element);
    }

    public LineMarkerInfo<?> protoToImpl(@NotNull PsiElement element) {

        //判断当前节点是否未keyword
        if (!(element instanceof ProtoLeafElement))
            return null;

        if (!(((ProtoLeafElement) element).getElementType() instanceof ProtoKeywordTokenType)) {
            return null;
        }

        String methodName = null,serviceName;
        String text = element.getText();
        switch (text) {
            case "service":
                serviceName = ((PbServiceDefinitionImpl) element.getParent()).getName();
                break;
            case "rpc":
                if (!(element.getParent() instanceof PbServiceMethodImpl)) {
                    return null;
                }
                methodName = ((PbServiceMethodImpl) element.getParent()).getName();
                serviceName = ((PbServiceDefinitionImpl) element.getParent().getParent().getParent()).getName();
                break;
            default:
                return null;
        }

        PsiFile file = element.getContainingFile();
        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder
                .create(AllIcons.Gutter.ImplementedMethod)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTooltipText("点击跳转实现（如无，则创建）")
                .setTargets(new ArrayList<>());

        String finalMethodName = methodName;
        String finalServiceName = serviceName;
        return builder.createLineMarkerInfo(element, (e1, elt) -> {
            //没有实现，则创建实现
            String protoServicePath = platformService.getTripleServiceName(file, finalServiceName);
            MethodResult openResult = openImplFile(element.getProject(), protoServicePath,serviceName, finalMethodName);
            if (openResult.isSuccess())
                return;
            if (openResult.getCode().equals(ResultCode.IMPL_NOT_EXISTS)) {
                //实现类不存在，创建实现类
                PsiElement parent = elt.getParent();
                if (parent == null)
                    return;

                PbServiceBodyImpl body = PsiTreeUtil.getChildOfType(parent, PbServiceBodyImpl.class);
                if (body == null) {
                    NotifyService.warning(project,"没有找到当前Service的实现，请先实现Service");
                    return;
                }

                protoImplService.createImpl(element.getProject(), body, finalServiceName, (x) -> {
                    openImplFile(element.getProject(), protoServicePath,serviceName, finalMethodName);
                });
            } else if (openResult.getCode().equals(ResultCode.IMPL_METHOD_NOT_EXISTS)) {
                //方法不存在，创建方法
                PbServiceMethodImpl method = ((PbServiceMethodImpl) elt.getParent());
                protoImplService.insertMethod(element.getProject(), method, protoServicePath, () -> {
                    openImplFile(element.getProject(), protoServicePath,serviceName, finalMethodName);
                });
            }
        });
    }

    //打开实现
    public MethodResult openImplFile(Project project, String protoServicePath,String serviceName, String methodName) {
        if (!protoImplService.contains(protoServicePath)) {
            return MethodResult.fail(ResultCode.IMPL_NOT_EXISTS);
        }
        //PSI 不可信，需要重建
        File file = new File(protoImplService.get(protoServicePath));
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
        if (virtualFile == null) {
            return MethodResult.fail(ResultCode.IMPL_NOT_EXISTS);
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);

        if (StringUtils.isBlank(methodName)) {
            PsiElement clazz = platformService.getClass(psiFile,serviceName);
            if (clazz == null)
                return MethodResult.fail(ResultCode.IMPL_NOT_EXISTS);
            PsiNavigateUtil.navigate(clazz);
            return MethodResult.success();
        } else {
            PsiElement method = platformService.getMethod(psiFile,serviceName, methodName);
            if (method == null)
                return MethodResult.fail(ResultCode.IMPL_METHOD_NOT_EXISTS);
            PsiNavigateUtil.navigate(method);
            return MethodResult.success();
        }
    }

}
