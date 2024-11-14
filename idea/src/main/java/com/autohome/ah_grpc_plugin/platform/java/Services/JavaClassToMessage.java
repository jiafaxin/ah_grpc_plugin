package com.autohome.ah_grpc_plugin.platform.java.Services;

import com.autohome.ah_grpc_plugin.platform.java.common.JavaProtoUtils;
import com.autohome.ah_grpc_plugin.utils.ProtoNameUtils;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaClassToMessage {

    Project project;
    PsiClass clazz;
    PsiType[] fxs;

    Map<String,String> fields = new LinkedHashMap<>();
    List<JavaClassToMessage> classes = new ArrayList<>();

    String name;

    public JavaClassToMessage(PsiClass clazz){
        this.clazz = clazz;
        this.name = this.clazz.getName();
        project = clazz.getProject();
        buildField();
    }

    public JavaClassToMessage(PsiClass clazz,PsiType[] fxs){
        this.clazz = clazz;
        this.fxs = fxs;
        this.name = this.clazz.getName();
        project = clazz.getProject();
        buildField();
    }

    void buildField() {
        List<String> ps = new ArrayList<>();
        for (PsiTypeParameter typeParameter : clazz.getTypeParameters()) {
            ps.add(typeParameter.getName());
        }

        for (PsiField field : clazz.getFields()) {
            try {
                PsiType type = field.getType();
                if(type==null || type.getCanonicalText()==null){
                    continue;
                }
                if(ps.contains(type.getCanonicalText())){
                    type = fxs[ps.indexOf(type.getCanonicalText())];
                }
                JavaProtoUtils.buildType(field.getName(),type,fields,classes,false);
            }catch (Exception e){
                System.out.println(e);
            }
        }
    }

    public String toString() {
        return toString(0);
    }

    public String toString(int deep) {
        StringBuilder sb = new StringBuilder();
        AtomicInteger i = new AtomicInteger(1);

        for (int i1 = 0; i1 < deep; i1++) {
            sb.append(" ");
        }
        sb.append("message " + ProtoNameUtils.getBaseName(name,false) + "{\n");

        int finalDeep = deep;
        fields.forEach((k, v) -> {
            for (int i1 = 0; i1 < finalDeep; i1++) {
                sb.append(" ");
            }
            String fieldName = ProtoNameUtils.getFieldName(k);

            sb.append("  " + v + " " + fieldName + " = " + (i.getAndIncrement()) + "");

            if(!fieldName.equals(k)) {
                sb.append(" [json_name = \""+k+"\"]");
            }

            sb.append(";\n");
        });

        for (JavaClassToMessage object : classes) {
            sb.append("\n");
            sb.append(object.toString(deep+1));
            sb.append("\n");
        }

        for (int i1 = 0; i1 < deep; i1++) {
            sb.append(" ");
        }
        sb.append("}");
        return sb.toString();
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public List<JavaClassToMessage> getClasses() {
        return classes;
    }

    public PsiClass getClazz() {
        return clazz;
    }

    public String getName() {
        return name;
    }

    public boolean equal(JavaClassToMessage other){
        if(other.getName().equals(getName())){
            return true;
        }
        return false;
    }
}
