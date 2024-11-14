package com.autohome.ah_grpc_plugin.models;

import com.intellij.psi.PsiElement;

public class ProtoLineMarkerInfo {
    String servicePath;
    String methodName;

    PsiElement lineOn;

    public String getServicePath() {
        return servicePath;
    }

    public void setServicePath(String servicePath) {
        this.servicePath = servicePath;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public PsiElement getLineOn() {
        return lineOn;
    }

    public void setLineOn(PsiElement lineOn) {
        this.lineOn = lineOn;
    }
}
