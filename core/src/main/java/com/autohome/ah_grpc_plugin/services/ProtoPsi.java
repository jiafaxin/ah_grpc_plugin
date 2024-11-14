package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.models.ProtoGoPackageInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.protobuf.lang.psi.PbFile;
import com.intellij.protobuf.lang.psi.PbImportStatement;
import com.intellij.protobuf.lang.psi.PbMessageDefinition;
import com.intellij.protobuf.lang.psi.impl.PbImportStatementImpl;
import com.intellij.protobuf.lang.psi.impl.PbOptionStatementImpl;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ProtoPsi {
    PbFile file;

    public static ProtoPsi getInstance(PbFile pbFile) {
        ProtoPsi result = new ProtoPsi();
        result.setFile(pbFile);
        return result;
    }

    public static ProtoPsi getInstance(Project project, String path) {
        VirtualFile vf = VirtualFileService.get(Config.getInstance(project).protoBasePath() + path);
        if(vf==null)
            return null;
        PsiFile psiFile = PsiManager.getInstance(project).findFile(vf);
        if(psiFile == null || !(psiFile instanceof PbFile))
            return null;
        return getInstance((PbFile)PsiManager.getInstance(project).findFile(vf));
    }

    public String getPackage(){
        return file.getPackageQualifiedName().toString();
    }

    public List<ProtoGoPackageInfo> getGoPackages(Project project){
        List<ProtoGoPackageInfo> result = new ArrayList<>();
        String publicPackage = getPackage();
        String goPackage = getPackage("go_package");
        if(StringUtils.isNotBlank(goPackage)) {
            String[] goPackages = goPackage.split(";");
            if(goPackages.length>1) {
                ProtoGoPackageInfo info = new ProtoGoPackageInfo(goPackages[0], goPackages[1], publicPackage);
                result.add(info);
            }
        }
        for (ProtoPsi importProto : getImportProtos(project)) {
            result.addAll(importProto.getGoPackages(project));
        }
        return result;
    }

    public String getPackage(String packageType) {
        String packagename = "";
        List<PbOptionStatementImpl> options = PsiTreeUtil.getChildrenOfTypeAsList(file, PbOptionStatementImpl.class);
        for (PbOptionStatementImpl option : options) {
            if (option.getOptionExpression().getOptionName().getText().equals(packageType)) {
                if (option.getOptionExpression().getStringValue() == null)
                    continue;
                packagename = option.getOptionExpression().getStringValue().getValue();
                break;
            }
        }
        return packagename;
    }


    public PbMessageDefinition getMessageByName(Project project,String name) {
        if(!name.startsWith("autohome.")) {
            name = getPackage() + "." + name;
        }

        return getMessageByPathAndName(project,name);
    }


    public PbMessageDefinition getMessageByPathAndName(Project project, String pathAndName) {
        for (PbMessageDefinition message : getMessages()) {
            if (getMessagePathAndName(message).equals(pathAndName))
                return message;
        }
        List<ProtoPsi> importants = getImportProtos(project);
        if (importants == null || importants.size() == 0)
            return null;
        for (ProtoPsi important : importants) {
            PbMessageDefinition messageDefinition = important.getMessageByPathAndName(project,pathAndName);
            if (messageDefinition != null)
                return messageDefinition;
        }
        return null;
    }

    String getMessagePathAndName(PbMessageDefinition message){
        return getPackage() + "." + message.getName();
    }

    List<ProtoPsi> getImportProtos(Project project) {
        List<PbImportStatementImpl> imports = getImports();
        List<ProtoPsi> result = new ArrayList<>();
        for (PbImportStatementImpl anImport : imports) {
            ProtoPsi psi = getInstance(project, anImport.getImportName().getStringValue().getValue());
            if (psi == null)
                continue;
            result.add(psi);
        }
        return result;
    }
    public List<PbImportStatementImpl> getImports(){
        List<PbImportStatementImpl> imports = PsiTreeUtil.getChildrenOfAnyType(file, PbImportStatementImpl.class);
        return imports;
    }


    public List<PbMessageDefinition> getMessages(){
        return PsiTreeUtil.getChildrenOfAnyType(file, PbMessageDefinition.class);
    }

    public List<PbMessageDefinition> getImportMessages(Project project){
        List<PbMessageDefinition> messageDefinitions = new ArrayList<>();
        for (ProtoPsi importProto : getImportProtos(project)) {
            messageDefinitions.addAll(getMessages());
        }
        return messageDefinitions;
    }




    public PbFile getFile() {
        return file;
    }

    public void setFile(PbFile file) {
        this.file = file;
    }
}
