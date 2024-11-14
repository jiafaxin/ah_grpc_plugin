package com.autohome.ah_grpc_plugin.platform.java.Services;

import com.autohome.ah_grpc_plugin.platform.java.common.JavaProtoUtils;
import com.autohome.ah_grpc_plugin.utils.ProtoNameUtils;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.impl.source.PsiClassReferenceType;

import java.util.*;

public class JavaMethodToProtobuf {

    Project project;

    PsiMethod method;

    String methodName;

    Map<String,String> responseFields = new LinkedHashMap<>();

    List<JavaClassToMessage> responseClasses = new ArrayList<>();



    Map<String,String> requestFields = new LinkedHashMap<>();

    List<JavaClassToMessage> requestClasses = new ArrayList<>();


    public JavaMethodToProtobuf(PsiMethod method){
        this.project = method.getProject();
        this.method = method;
        this.methodName = ProtoNameUtils.getBaseName(method.getName(), false);
        buildRequest();
        buildResponse();
    }

    void buildRequest() {
        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            JavaProtoUtils.buildType(parameter.getName(), parameter.getType(), requestFields, requestClasses);
        }
    }

    void buildResponse(){
        PsiType type = method.getReturnType();
        responseFields.put("return_code", "int32");
        responseFields.put("return_msg", "string");
        JavaProtoUtils.buildType("result",type,responseFields,responseClasses);
        List<String> keys = new ArrayList<>();
        responseFields.forEach((k,v)->{
            if(k.toLowerCase().equals("returncode") || k.toLowerCase().equals("message")){
                keys.add(k);
            }
        });
        for (String key : keys) {
            responseFields.remove(key);
        }
    }



    public String rpcMethod() {
        StringBuilder sb = new StringBuilder();

        sb.append("  /**\n" +
                "    * 接口说明\n" +
                "    * 维护人:\n" +
                "    */\n");
        sb.append("  rpc ").append(methodName).append("(").append(methodName).append("Request) returns (").append(methodName).append("Response){}\n");

        return sb.toString();
    }

    public String responseMessage() {
        return JavaProtoUtils.buildMessage(methodName + "Response", 0, responseFields, responseClasses);
    }

    public String requestMessage(){
        return JavaProtoUtils.buildMessage(methodName + "Request",0,requestFields,requestClasses);
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("service SampleService{\n");
        sb.append(rpcMethod());
        sb.append("}");
        sb.append("\n\n");

        sb.append(requestMessage());
        sb.append("\n\n");

        sb.append(responseMessage());
        sb.append("\n\n");

        return sb.toString();
    }




}
