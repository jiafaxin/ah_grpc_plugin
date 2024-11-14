package com.autohome.ah_grpc_plugin.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.protobuf.lang.psi.impl.PbOptionStatementImpl;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.List;

public class ProtoPsiUtils {

    public static String getJavaPackage(PsiFile file) {
        return getPackage(file,"java_package");
    }

    public static String getGoPackage(PsiFile file) {
        return getPackage(file,"go_package");
    }

    public static String getPackage(PsiFile file,String packageType) {
        return ApplicationManager.getApplication().runReadAction((Computable<String>)()->{
            String packagename = "";
            List<PbOptionStatementImpl> options = PsiTreeUtil.getChildrenOfTypeAsList(file, PbOptionStatementImpl.class);
            for (PbOptionStatementImpl option : options) {
                if (option.getOptionExpression().getOptionName().getText().equals(packageType)) {
                    if (option.getOptionExpression().getStringValue() == null)
                        break;
                    packagename = option.getOptionExpression().getStringValue().getValue();
                    break;
                }
            }
            return packagename;
        });
    }
}
