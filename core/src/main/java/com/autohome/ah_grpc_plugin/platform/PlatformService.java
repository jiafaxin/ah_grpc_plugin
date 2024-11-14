package com.autohome.ah_grpc_plugin.platform;

import com.autohome.ah_grpc_plugin.models.ImplClassInfo;
import com.autohome.ah_grpc_plugin.models.MethodResult;
import com.autohome.ah_grpc_plugin.models.ProtoLineMarkerInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.protobuf.lang.psi.impl.PbServiceBodyImpl;
import com.intellij.protobuf.lang.psi.impl.PbServiceMethodImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import java.util.Collection;
import java.util.List;

public interface PlatformService {

    String getTripleServiceName(PsiFile file,String serviceName);

    boolean isImplFile(VirtualFile virtualFile);

    Collection<VirtualFile> findAllFiles(Project project);

    PsiElement getClass(PsiFile file,String serviceName);

    PsiElement getMethod(PsiFile psiFile,String serviceName,String methodName);

    List<String> getAllExtendTypes(PsiFile simpleFile);

    ProtoLineMarkerInfo getProtoLineMarkerInfo(PsiElement element);

    PsiFile createFile(Project project,String serviceName,PbServiceBodyImpl serviceBody,String savePath);

    MethodResult insertMethod(Project project, PsiFile psiFile, PbServiceMethodImpl method, Runnable callback);

    String methodName(String oName);

    void refreshRoot(Project project);

}
