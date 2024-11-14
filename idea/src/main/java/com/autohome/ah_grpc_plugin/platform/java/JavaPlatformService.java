package com.autohome.ah_grpc_plugin.platform.java;

import com.autohome.ah_grpc_plugin.enums.ResultCode;
import com.autohome.ah_grpc_plugin.models.ImplClassInfo;
import com.autohome.ah_grpc_plugin.models.JavaMethodInfo;
import com.autohome.ah_grpc_plugin.models.MethodResult;
import com.autohome.ah_grpc_plugin.models.ProtoLineMarkerInfo;
import com.autohome.ah_grpc_plugin.platform.PlatformService;
import com.autohome.ah_grpc_plugin.services.ProtoPsi;
import com.autohome.ah_grpc_plugin.utils.ProtoNameUtils;
import com.autohome.ah_grpc_plugin.utils.ProtoPsiUtils;
import com.intellij.ide.actions.SynchronizeCurrentFileAction;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.protobuf.lang.psi.*;
import com.intellij.protobuf.lang.psi.impl.PbFileImpl;
import com.intellij.protobuf.lang.psi.impl.PbOptionStatementImpl;
import com.intellij.protobuf.lang.psi.impl.PbServiceBodyImpl;
import com.intellij.protobuf.lang.psi.impl.PbServiceMethodImpl;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.rd.util.reactive.KeyValuePair;
import io.grpc.protobuf.ProtoUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.dom.model.MavenDomModules;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.project.actions.UpdateFoldersAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class JavaPlatformService implements PlatformService {

    public ImplClassInfo getContent(Project project, String protoName,PbServiceBodyImpl body,String savePath){

        String pk = savePath.substring(savePath.indexOf("src/main/java")+14).replace("/",".");

        ImplClassInfo classInfo = new ImplClassInfo();
        StringBuilder result = new StringBuilder();

        PbFileImpl pbFile = getPbFile(body);
        String javaPackage = getJavaPackage(pbFile);
        String serviceName = protoName;
        String tripleName = String.format("Dubbo%sTriple",serviceName);

        List<JavaMethodInfo> methods = new ArrayList<>();
        for (PbServiceMethod method : body.getServiceMethodList()) {
            methods.add(getMethodContent(project, method,true,javaPackage));
        }

        result.append("package "+ pk + ";\n");
        result.append("\n");
        result.append("import org.apache.dubbo.config.annotation.DubboService;\n");
        result.append(String.format("import %s.*;\n",javaPackage));
//        for (JavaMethodInfo method : methods) {
//            result.append(String.format("import %s;\n",method.getRequestPackage()));
//            result.append(String.format("import %s;\n",method.getResponsePackage()));
//        }
        result.append("\n");
        result.append("@DubboService\n");
        result.append(String.format("public class %sGrpcImpl extends %s.%sImplBase {\n",serviceName,tripleName,serviceName));

        result.append("\n");

        classInfo.setServiceName(serviceName);
        classInfo.setPackageName(javaPackage);

        if(body.getServiceMethodList().size()==0)
            return classInfo;

        for (JavaMethodInfo method : methods) {
            result.append(method.getContent());
        }

        result.append("}");

        classInfo.setContent(result.toString());
        return classInfo;
    }

    public JavaMethodInfo getMethodContent(Project project, PbServiceMethod method, boolean isFormat, String basePackage) {
        StringBuilder result = new StringBuilder();
        String methodName = method.getName();
        List<PbServiceMethodType> types = method.getServiceMethodTypeList();
        PbServiceMethodType responseType = types.get(1);

        KeyValuePair<String, String> request = getMessageInfo(basePackage, types.get(0).getText());
        KeyValuePair<String, String> response = getMessageInfo(basePackage, responseType.getText());

        ProtoPsi psi = ProtoPsi.getInstance(method.getPbFile());
        PbMessageDefinition message = psi.getMessageByName(project, response.getKey());

        result.append(isFormat ? "    " : "").append("@Override\n");
        result.append(String.format("    public %s %s(%s request) {\n", response.getValue(), ProtoNameUtils.javaMethodName(methodName), request.getValue()));
        result.append(getResponse(message, response.getValue()));
        result.append("    }\n");
        result.append("\n");
        JavaMethodInfo methodInfo = new JavaMethodInfo();
        methodInfo.setContent(result.toString());
        methodInfo.setRequestPackage(request.getKey());
        methodInfo.setResponsePackage(response.getKey());
        return methodInfo;
    }

    KeyValuePair<String,String> getMessageInfo(String basePackage,String name) {
        if (name.startsWith("autohome.rpc.")) {
            int splitIndex = name.lastIndexOf(".");
            return new KeyValuePair<>(name, name.substring(splitIndex + 1));
        }
        return new KeyValuePair<>(basePackage + "." + name, name);
    }

    String getResponse(PbMessageDefinition message,String messageName) {
        if(message == null)
            return "responseError";
        StringBuilder fs = new StringBuilder();
        fs.append("        return " + messageName + ".newBuilder()\n");
        for (PbSimpleField field : message.getBody().getSimpleFieldList()) {
            String fieldName = ProtoNameUtils.toPascal(field.getName());
            fs.append("                //.set" + fieldName + "(x.get" + fieldName + ")\n");
        }
        fs.append("                .build();\n");
        return fs.toString();
    }

    PbFileImpl getPbFile(PsiElement psiElement){
        return (PbFileImpl)psiElement.getContainingFile();
//        PbFileImpl pbFile = null;
//        PsiElement e = psiElement.getParent();
//        while (true){
//            if(e instanceof PbFileImpl) {
//                pbFile = (PbFileImpl)e;
//                break;
//            }
//            e= e.getParent();
//        }
//        return pbFile;
    }

    String getJavaPackage(PbFile pbFile){
        List<PbOptionStatementImpl> options = PsiTreeUtil.getChildrenOfTypeAsList(pbFile, PbOptionStatementImpl.class);
        String javaPackage = "";
        for (PbOptionStatementImpl option : options) {
            PbOptionExpression expression = option.getOptionExpression();
            if(!expression.getOptionName().getText().equals("java_package")){
                continue;
            }
            javaPackage = expression.getStringValue().getText();
            javaPackage = javaPackage.substring(1,javaPackage.length()-1);
            break;
        }
        return javaPackage;
    }

    public boolean isProtoTriple(String className) {
        if (!className.startsWith("autohome.rpc."))
            return false;

        String[] l = className.split("[.]");

        if (!(l[l.length-2].startsWith("Dubbo")))
            return false;

        if (!(l[l.length-2].endsWith("Triple")))
            return false;

        if (!l[l.length-1].endsWith("ImplBase"))
            return false;
        return true;
    }

    @Override
    public Collection<VirtualFile> findAllFiles(Project project) {
        return  FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project));
    }

    @Override
    public PsiElement getClass(PsiFile file,String serviceName) {
        PsiClassImpl clazz = PsiTreeUtil.getChildOfType(file, PsiClassImpl.class);
        return clazz;
    }

    @Override
    public PsiElement getMethod(PsiFile psiFile,String serviceName,String methodName) {
        PsiElement clazz = getClass(psiFile,"");
        for (PsiMethod method : ((PsiClassImpl)clazz).getMethods()) {
            if(method.getName().equals(methodName(methodName))){
                return method;
            }
        }
        return null;
    }

    @Override
    public List<String> getAllExtendTypes(PsiFile simpleFile) {

        List<String> result = new ArrayList<>();
        //查找类节点
        PsiElement clazz = getClass(simpleFile,"");
        if (clazz == null)
            return result;

        //通过注解判断是否实现类
        if(!isImplClass(clazz)){
            return result;
        }

        //所有父类
        PsiType[] extendTypes = ((PsiClassImpl)clazz).getExtendsListTypes();

        if (extendTypes.length == 0)
            return result;

        for (PsiType extendType : extendTypes) {
            //父类的包名
            List<String> packageNames = getRealExtendType(simpleFile, extendType);
            for (String packageName : packageNames) {
                if (!isProtoTriple(packageName))
                    continue;
                result.add(packageName);
            }
        }
        return result;
    }

    public List<String> getRealExtendType(PsiFile simpleFile,PsiType psiType) {
        String name = psiType.getCanonicalText();
        if (name.startsWith("autohome.rpc"))
            return Arrays.asList(name);
        PsiJavaFile file = (PsiJavaFile) simpleFile;
        int di = name.lastIndexOf(".");
        String eName = di > 0 ? name.substring(0, di) : name;
        for (PsiImportStatement importStatement : file.getImportList().getImportStatements()) {
            String importName = importStatement.getQualifiedName();
            int ii = importName.lastIndexOf(".");
            String rName = importName.substring(ii + 1);
            if (rName.equals(eName))
                return Arrays.asList(importName.substring(0, ii + 1) + name);
        }
        List<String> names = new ArrayList<>();
        for (PsiImportStatement importStatement : file.getImportList().getImportStatements()) {
            String importName = importStatement.getQualifiedName();
            if (!importStatement.getText().endsWith(".*;")) {
                continue;
            }
            names.add(importName + "." + name);
            return names;
        }
        return new ArrayList<>();
    }

    @Override
    public ProtoLineMarkerInfo getProtoLineMarkerInfo(PsiElement element) {
        String methodName = "";
        String protoServicePath = "";
        PsiClassType[] referenceTypes;
        PsiElement lineOn;
        if((element instanceof PsiClassImpl)) {
            PsiReferenceList psiReferenceList = ((PsiClassImpl) element).getExtendsList();
            if(psiReferenceList==null)
                return null;
            referenceTypes =psiReferenceList.getReferencedTypes();
            lineOn = PsiTreeUtil.getChildOfType(element, PsiIdentifier.class);
        }else if(element instanceof PsiMethod){
            methodName = ((PsiMethod) element).getName();
            lineOn = PsiTreeUtil.getChildOfType(element,PsiIdentifier.class);
            if(element.getParent()==null)
                return null;
            PsiClassImpl psiClassImpl = (PsiClassImpl) element.getParent();
            if(psiClassImpl.getExtendsList()==null)
                return null;
            referenceTypes = psiClassImpl.getExtendsList().getReferencedTypes();
        }else{
            return null;
        }

        if(referenceTypes==null||referenceTypes.length==0)
            return null;

        for (PsiClassType referenceType : referenceTypes) {
            String clazz = referenceType.getCanonicalText();
            if(isProtoTriple(clazz)) {
                protoServicePath = clazz;
                break;
            }
        }
        if(StringUtils.isBlank(protoServicePath))
            return null;

        ProtoLineMarkerInfo result = new ProtoLineMarkerInfo();
        result.setLineOn(lineOn);
        result.setServicePath(protoServicePath);
        result.setMethodName(methodName);
        return result;
    }

    @Override
    public PsiFile createFile(Project project,String serviceName,PbServiceBodyImpl serviceBody,String savePath) {
        ImplClassInfo content = getContent(project,serviceName,serviceBody,savePath);
        String fileName = serviceName+"GrpcImpl.java";
        PsiFile file = PsiFileFactory.getInstance(project).createFileFromText(fileName, JavaFileType.INSTANCE,content.getContent());
        return file;
    }

    @Override
    public MethodResult insertMethod(Project project, PsiFile psiFile, PbServiceMethodImpl method, Runnable callback) {
        PsiClassImpl clazz = PsiTreeUtil.getChildOfType(psiFile, PsiClassImpl.class);
        if (clazz == null)
            return MethodResult.fail(ResultCode.IMPL_NOT_EXISTS);

        PsiElementFactory factory = PsiElementFactory.getInstance(project);

        PbFile pbFile = method.getPbFile();
        String javaPackage = getJavaPackage(pbFile);

//        PsiImportStatement[] imports = ((PsiJavaFile) psiFile).getImportList().getImportStatements();
//        PsiElement lastImportant = null;
//        if(imports == null || imports.length == 0){
//            lastImportant = ((PsiJavaFile) psiFile).getPackageStatement();
//        }else{
//            lastImportant = imports[imports.length-1];
//        }

        JavaMethodInfo methodInfo = getMethodContent(project, method, false,javaPackage);
        PsiMethod nm = factory.createMethodFromText(methodInfo.getContent(), null);
//        PsiElement finalLastImportant = lastImportant;
        ApplicationManager.getApplication().runWriteAction(() -> {
            clazz.add(nm);
//            finalLastImportant.getParent().addAfter(factory.createImportStatementOnDemand(methodInfo.getRequestPackage()), finalLastImportant);
//            finalLastImportant.getParent().addAfter(factory.createImportStatementOnDemand(methodInfo.getResponsePackage()), finalLastImportant);
            callback.run();
        });
        return MethodResult.success();
    }

    @Override
    public String methodName(String oName) {
        return ProtoNameUtils.javaMethodName(oName);
    }

    @Override
    public void refreshRoot(Project project) {
        MavenProjectsManager.getInstance(project).scheduleFoldersResolveForAllProjects();
    }

    public String getTripleServiceName(PsiFile file,String serviceName){
        String packageName =  ProtoPsiUtils.getJavaPackage(file);
        return packageName.concat(String.format(".Dubbo%sTriple.%sImplBase",serviceName,serviceName));
    }

    public boolean isImplClass(PsiElement clazzz) {
        if (!(clazzz instanceof PsiClassImpl)) {
            return false;
        }
        PsiClassImpl clazz = (PsiClassImpl) clazzz;
        PsiAnnotation[] annotations = clazz.getAnnotations();
        if (annotations == null || annotations.length == 0)
            return false;

        try {
            if (!Arrays.stream(annotations).anyMatch(x -> x.getQualifiedName().equals("org.apache.dubbo.config.annotation.DubboService"))) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isImplFile(VirtualFile virtualFile) {
        return virtualFile.getFileType() instanceof JavaFileType;
    }


}
