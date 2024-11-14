package com.autohome.ah_grpc_plugin.platform.go;

import com.autohome.ah_grpc_plugin.models.ImplClassInfo;
import com.autohome.ah_grpc_plugin.models.MethodResult;
import com.autohome.ah_grpc_plugin.models.ProtoGoPackageInfo;
import com.autohome.ah_grpc_plugin.models.ProtoLineMarkerInfo;
import com.autohome.ah_grpc_plugin.platform.PlatformService;
import com.autohome.ah_grpc_plugin.services.ProtoPsi;
import com.autohome.ah_grpc_plugin.utils.ProtoNameUtils;
import com.autohome.ah_grpc_plugin.utils.ProtoPsiUtils;
import com.goide.GoFileType;
import com.goide.GoLanguage;
import com.goide.psi.*;
import com.goide.psi.impl.GoFieldDeclarationImpl;
import com.goide.psi.impl.GoMethodDeclarationImpl;
import com.goide.psi.impl.GoTypeDeclarationImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.protobuf.lang.psi.*;
import com.intellij.protobuf.lang.psi.impl.PbServiceBodyImpl;
import com.intellij.protobuf.lang.psi.impl.PbServiceDefinitionImpl;
import com.intellij.protobuf.lang.psi.impl.PbServiceMethodImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class GoPlatformService implements PlatformService {

    @Override
    public String getTripleServiceName(PsiFile file, String serviceName) {
        String packageName =  ProtoPsiUtils.getGoPackage(file);
        return packageName.concat(String.format("/Unimplemented%sServer",serviceName));
    }

    public ImplClassInfo getContent(Project project, String protoName, PbServiceBodyImpl body,String savePath) {

        PbServiceDefinitionImpl service = (PbServiceDefinitionImpl) body.getParent();
        String serviceName = service.getName();

        ImplClassInfo classInfo = new ImplClassInfo();
        StringBuilder result = new StringBuilder();

        PbFile file = body.getPbFile();

        ProtoPsi psi = ProtoPsi.getInstance(file);
        List<ProtoGoPackageInfo> packageInfos = psi.getGoPackages(project);

        String packageName = ProtoPsiUtils.getGoPackage(file);
        String[] packages = packageName.split(";");

        String filePackage = savePath.substring(savePath.lastIndexOf("/")+1);

        result.append("package "+filePackage+"\n");
        result.append("\n");
        result.append("import (\n");
        result.append("\t\"context\"\n");
        result.append("\t\"dubbo.apache.org/dubbo-go/v3/config\"\n");
        for (ProtoGoPackageInfo packageInfo : packageInfos) {
            result.append("\t\""+packageInfo.getGoPackage()+"\" \n");
        }
        result.append(")\n");
        result.append("\n");
        result.append("func init() {\n");
        result.append("	config.SetProviderService(&"+serviceName+"{})\n");
        result.append("}\n");
        result.append("\n");
        result.append("type "+serviceName+" struct {\n");
        result.append("	"+packages[1]+".Unimplemented"+serviceName+"Server\n");
        result.append("}\n");
        result.append("\n");

        for (PbServiceMethod method : body.getServiceMethodList()) {
            result.append(getMethodContent(project, method,packages[1],serviceName,packageInfos));
        }

        result.append("\n");
        classInfo.setContent(result.toString());
        return classInfo;

    }

    public String getMethodContent(Project project, PbServiceMethod method,String packageName,String serviceName,List<ProtoGoPackageInfo> packageInfos) {
        List<PbServiceMethodType> types = method.getServiceMethodTypeList();
        String request = types.get(0).getText();
        PbServiceMethodType responseType = types.get(1);
        String response = responseType.getText();

        StringBuilder sb = new StringBuilder();
        String methodName = method.getName();
        sb.append("func (" + serviceName + ") " + methodName + "(context.Context, *" + getRealName(request, packageName,packageInfos) + ") (*" + getRealName(response, packageName,packageInfos) + ", error) {\n");
        sb.append("	return &" + getRealName(response, packageName,packageInfos) + "{\n");
        ProtoPsi psi = ProtoPsi.getInstance(method.getPbFile());
        PbMessageDefinition message = psi.getMessageByName(project, response);
        for (PbSimpleField field : message.getBody().getSimpleFieldList()) {
            String fieldName = ProtoNameUtils.toPascal(field.getName());
            sb.append("                //" + fieldName + ": nil,\n");
        }
        sb.append("	}, nil\n");
        sb.append("}\n");
        return sb.toString();
    }

    public String getRealName(String name,String baseName,List<ProtoGoPackageInfo> packageInfos) {
        if (name.startsWith("autohome.")) {
            int splitIndex = name.lastIndexOf(".");
            String packageName = name.substring(0, name.lastIndexOf("."));
            name = name.substring(splitIndex+1);

            for (ProtoGoPackageInfo packageInfo : packageInfos) {
                if (packageInfo.getPublicPackage().equals(packageName)) {
                    return packageInfo.getName().concat(".").concat(name);
                }
            }
        }
        return baseName.concat(".").concat(name);
    }


    @Override
    public boolean isImplFile(VirtualFile virtualFile) {
        return virtualFile.getFileType() instanceof GoFile;
    }

    @Override
    public Collection<VirtualFile> findAllFiles(Project project) {
        return  FileTypeIndex.getFiles(GoFileType.INSTANCE, GlobalSearchScope.projectScope(project));
    }

    @Override
    public PsiElement getClass(PsiFile file,String serviceName) {
        if(!(file instanceof GoFile))
            return null;
        GoFile goFile = (GoFile) file;
        if(StringUtils.isBlank(serviceName))
            return goFile.getTypes().stream().findFirst().orElse(null);
        return goFile.getTypes(serviceName).stream().findFirst().orElse(null);
    }

    @Override
    public PsiElement getMethod(PsiFile psiFile,String serviceName, String methodName) {
        GoFile file = (GoFile) psiFile;
        for (GoMethodDeclaration method : file.getMethods()) {
            if (method.getReceiverType().getText().equals(serviceName) && method.getIdentifier().getText().equals(methodName(methodName))) {
                return method;
            }
        }
        return null;
    }

    @Override
    public List<String> getAllExtendTypes(PsiFile simpleFile) {
        List<String> result = new ArrayList<>();
        if (!(simpleFile instanceof GoFile)) {
            return result;
        }

        GoFile file = (GoFile) simpleFile;

        Map<String, String> packages = getPackages(file);

        if(packages.size()==0)
            return result;

        for (GoTypeSpec type : ((GoFile) simpleFile).getTypes()) {
            Collection<GoFieldDeclarationImpl> impls = PsiTreeUtil.findChildrenOfType(type, GoFieldDeclarationImpl.class);
            if (impls == null || impls.size() == 0)
                continue;

            for (GoFieldDeclarationImpl impl : impls) {
                GoTypeReferenceExpression expression = impl.getAnonymousFieldDefinition().getTypeReferenceExpression();
                String pa = expression.getQualifier().getText();
                String service = expression.getReference().getCanonicalText();
                if (!packages.containsKey(pa))
                    continue;
                result.add(packages.get(pa).concat(";").concat(pa).concat("/").concat(service));
            }
        }
        return result;
    }

    String getServicePath(GoTypeSpec type){
        Map<String, String> packages = getPackages(type.getContainingFile());
        Collection<GoFieldDeclarationImpl> impls = PsiTreeUtil.findChildrenOfType(type, GoFieldDeclarationImpl.class);
        if (impls == null || impls.size() == 0)
            return null;

        for (GoFieldDeclarationImpl impl : impls) {
            GoTypeReferenceExpression expression = impl.getAnonymousFieldDefinition().getTypeReferenceExpression();
            String pa = expression.getQualifier().getText();
            String service = expression.getReference().getCanonicalText();
            if (!packages.containsKey(pa))
                continue;
            return packages.get(pa).concat(";").concat(pa).concat("/").concat(service);
        }
        return null;
    }

    @Override
    public ProtoLineMarkerInfo getProtoLineMarkerInfo(PsiElement element) {
        if(element instanceof GoTypeDeclarationImpl){
            ProtoLineMarkerInfo info = new ProtoLineMarkerInfo();
            GoTypeSpec spec = ((GoTypeDeclarationImpl)element).getTypeSpecList().get(0);
            info.setLineOn(spec.getIdentifier());
            info.setServicePath(getServicePath(spec));
            return info;
        } else if(element instanceof GoMethodDeclarationImpl){
            GoMethodDeclarationImpl method = (GoMethodDeclarationImpl) element;
            GoTypeSpec spec = method.resolveTypeSpec();
            ProtoLineMarkerInfo info = new ProtoLineMarkerInfo();
            info.setLineOn(method.getIdentifier());
            info.setServicePath(getServicePath(spec));
            info.setMethodName(method.getIdentifier().getText());
            return info;
        }
        return null;
    }

    @Override
    public PsiFile createFile(Project project, String serviceName, PbServiceBodyImpl serviceBody,String savePath) {
        ImplClassInfo content = getContent(project,serviceName,serviceBody,savePath);
        String fileName = serviceName +"GrpcImpl.go";
        PsiFile file = PsiFileFactory.getInstance(project).createFileFromText(fileName, GoFileType.INSTANCE,content.getContent());
        return file;
    }

    @Override
    public MethodResult insertMethod(Project project, PsiFile psiFile, PbServiceMethodImpl method, Runnable callback) {
        PbServiceDefinitionImpl service = PsiTreeUtil.getParentOfType(method, PbServiceDefinitionImpl.class);
        String serviceName = service.getName();
        PbFile file = method.getPbFile();
        String packageName = ProtoPsiUtils.getGoPackage(file);
        String[] packages = packageName.split(";");
        List<ProtoGoPackageInfo> packageInfos = ProtoPsi.getInstance(file).getGoPackages(project);
        String methodContent = getMethodContent(project, method, packages[1], serviceName,packageInfos);
        ApplicationManager.getApplication().runWriteAction(() -> {
            psiFile.add(createMethod(project,methodContent));
            //格式化代码
            CodeStyleManager.getInstance(project).reformat(psiFile);
            callback.run();
        });
        return MethodResult.success();
    }

    GoMethodDeclaration createMethod(Project project,String content){
        content = "package demo\n" + content;
        GoFile newFile = (GoFile)PsiFileFactory.getInstance(project).createFileFromText(GoLanguage.INSTANCE, content);
        return newFile.getMethods().get(0);
    }

    final static String importStart = "git.corpautohome.com/microservice/go-genproto/autohome/rpc/";

    Map<String,String> getPackages(GoFile file){
        Map<String,String> result = new HashMap<>();
        for (GoImportSpec anImport : file.getImports()) {
            String path = anImport.getPath();
            if(!path.startsWith(importStart))
                continue;

            if(StringUtils.isNotBlank(anImport.getName())){
                result.put(anImport.getName(),path);
            }else{
                String name = getPackageByImport(path);
                if(name==null)
                    continue;
                result.put(name,path);
            }
        }
        return result;
    }

    String getPackageByImport(String importPath){
        importPath = importPath.substring(importStart.length());
        String[] paths = importPath.split("/");
        switch (paths.length){
            case 3:
                return paths[1];
            case 4:
                return paths[3];
        }
        return null;
    }

    @Override
    public String methodName(String oName) {
        return oName;
    }

    @Override
    public void refreshRoot(Project project) {

    }

}
