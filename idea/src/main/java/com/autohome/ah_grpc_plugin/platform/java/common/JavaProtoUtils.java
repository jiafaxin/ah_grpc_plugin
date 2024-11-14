package com.autohome.ah_grpc_plugin.platform.java.common;

import com.autohome.ah_grpc_plugin.platform.java.Services.JavaClassToMessage;
import com.autohome.ah_grpc_plugin.platform.java.Services.JavaMethodToProtobuf;
import com.autohome.ah_grpc_plugin.utils.ProtoNameUtils;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.impl.source.PsiClassReferenceType;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaProtoUtils {


    public static String buildMessage(String name, int deep, Map<String,String> fields, List<JavaClassToMessage> classList){
        StringBuilder sb = new StringBuilder();
        AtomicInteger i = new AtomicInteger(1);

        for (int i1 = 0; i1 < deep; i1++) {
            sb.append(" ");
        }
        sb.append("message " + name + " {\n");

        int finalDeep = deep;
        fields.forEach((k, v) -> {
            for (int i1 = 0; i1 < finalDeep; i1++) {
                sb.append(" ");
            }
            String fieldName = ProtoNameUtils.getFieldName(k);

            sb.append("  " + v + " " + fieldName + " = " + (i.getAndIncrement()) + "");

            if(!fieldName.equals(k)) {
                sb.append(" [json_name = \""+k+"\"]");
            }else if(fieldName.equals("return_code")){
                sb.append(" [json_name = \"returncode\"]");
            }else if(fieldName.equals("return_msg")){
                sb.append(" [json_name = \"message\"]");
            }

            sb.append(";\n");
        });

        for (JavaClassToMessage object : classList) {
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

    public static void buildType(String name, PsiType type, Map<String,String> fields, List<JavaClassToMessage> classes){
        buildType(name,type,fields,classes,true);
    }

    public static void buildType(String name, PsiType type, Map<String,String> fields, List<JavaClassToMessage> classes,boolean addToParentIfClass){
        String typeName = getPrimitiveType(type);
        if(typeName==null){
            typeName = getClsClassType(type);
        }

        if(typeName!=null){
            fields.put(name,typeName);
            return;
        }

        TypeInfo typeInfo = getListInfo(type);
        if(typeInfo==null){
            typeInfo = getArrayType(type);
        }
        if(typeInfo==null){
            typeInfo = getMapInfo(type);
        }

        if(typeInfo!=null){
            fields.put(name,typeInfo.getTypeName());
            for (PsiClass picClass : typeInfo.getPicClasses()) {
                addClass(classes,new JavaClassToMessage(picClass));
            }
            return;
        }

        typeInfo = getPsiClassInfo(type);
        if(typeInfo!=null){
            JavaClassToMessage root = new JavaClassToMessage(typeInfo.getPicClasses().get(0),typeInfo.getFxs());
            if(addToParentIfClass) {
                fields.putAll(root.getFields());
                addClass(classes, root.getClasses());
            }else{
                fields.put(name,root.getName());
                addClass(classes,root);
            }
            return;
        }
    }

    static void addClass(List<JavaClassToMessage> classes,List<JavaClassToMessage> clazz){
        for (JavaClassToMessage javaClassToMessage : clazz) {
            addClass(classes,javaClassToMessage);
        }
    }

    static void addClass(List<JavaClassToMessage> classes,JavaClassToMessage clazz){
        if(classes.stream().anyMatch(x->x.equal(clazz)))
            return;
        classes.add(clazz);
    }

    static TypeInfo getArrayType(PsiType type){
        if(! (type instanceof PsiArrayType)){
            return null;
        }
        PsiType sType = ((PsiArrayType)type).getComponentType();

        String typeName = getPrimitiveType(sType);
        if (typeName != null) {
            return new TypeInfo("repeated " + typeName);
        }

        typeName = getClsClassType(sType);
        if (typeName != null) {
            return new TypeInfo("repeated " + typeName);
        }

        TypeInfo typeInfo = getPsiClassInfo(sType);
        if (typeInfo != null) {
            return new TypeInfo("repeated " + typeInfo.getTypeName(), typeInfo.getPicClasses());
        }
        return null;
    }

    static TypeInfo getListInfo(PsiType type) {
        if (!(type instanceof PsiClassReferenceType))
            return null;

        PsiClass rType = ((PsiClassReferenceType) type).resolve();
        if (!(rType instanceof PsiClass)) {
            return null;
        }
        if (!rType.getName().equals("List")) {
            return null;
        }

        PsiType sType = ((PsiClassReferenceType) type).getParameters()[0];
        String typeName = getPrimitiveType(sType);
        if (typeName != null) {
            return new TypeInfo("repeated " + typeName);
        }

        typeName = getClsClassType(sType);
        if (typeName != null) {
            return new TypeInfo("repeated " + typeName);
        }

        TypeInfo typeInfo = getPsiClassInfo(sType);
        if (typeInfo != null) {
            return new TypeInfo("repeated " + typeInfo.getTypeName(), typeInfo.getPicClasses());
        }
        return null;
    }


    static TypeInfo getMapInfo(PsiType type) {
        if (!(type instanceof PsiClassReferenceType))
            return null;

        PsiClass rType = ((PsiClassReferenceType) type).resolve();
        if (!(rType instanceof PsiClass)) {
            return null;
        }

        //判断是否是map
        if (!rType.getName().equals("Map")) {
            PsiType[] stypes = ((PsiClassReferenceType) type).getSuperTypes();
            if(!Arrays.stream(stypes).anyMatch(x->x.getCanonicalText().equals("java.util.Map"))) {
                return null;
            }
        }
        List<PsiClass> psiClasses = new ArrayList<>();
        PsiType[] types = ((PsiClassReferenceType) type).getParameters();
        if(types==null || types.length < 2) {
            return new TypeInfo("map<string,string>", psiClasses);
        }

        PsiType fType = types[0];
        String fTypeName = getPrimitiveType(fType);
        if (fTypeName == null) {
            fTypeName = getClsClassType(fType);
            if (fTypeName == null) {
                TypeInfo typeInfo = getPsiClassInfo(fType);
                if (typeInfo != null) {
                    fTypeName = typeInfo.getTypeName();
                    psiClasses.addAll(typeInfo.getPicClasses());
                }
            }
        }
        PsiType sType = types[1];
        String sTypeName = getPrimitiveType(sType);
        if (sTypeName == null) {
            sTypeName = getClsClassType(sType);
            if (sTypeName == null) {
                TypeInfo typeInfo = getPsiClassInfo(sType);
                if (typeInfo != null) {
                    sTypeName = typeInfo.getTypeName();
                    psiClasses.addAll(typeInfo.getPicClasses());
                }
            }
        }

        return new TypeInfo("map<" + fTypeName + "," + sTypeName + ">", psiClasses);
    }

    static TypeInfo getPsiClassInfo(PsiType type){
        if(!(type instanceof PsiClassReferenceType))
            return null;

        PsiClass rType = ((PsiClassReferenceType) type).resolve();

        if(rType instanceof ClsClassImpl){
            return null;
        }

        if(!(rType instanceof PsiClass)){
            return null;
        }
        String typeName = type.getPresentableText();
        Map<String,PsiType> fxs = new LinkedHashMap<>();
        if(typeName.indexOf("<")>0){
            //处理泛型
            String [] fxNames = typeName.substring(typeName.indexOf("<")+1,typeName.length()-1).split(",");
            for (int i = 0; i < fxNames.length; i++) {
                fxs.put(fxNames[i], ((PsiClassReferenceType) type).getParameters()[i]);
            }
        }
        return new TypeInfo(rType.getName(),((PsiClassReferenceType) type).getParameters(),rType);
    }


    static String getPrimitiveType(PsiType type){
        if(!(type instanceof PsiPrimitiveType))
            return null;

        switch (type.getCanonicalText()) {
            case "boolean":
                return "bool";
            case "short":
            case "int":
                return "int32";
            case "long":
                return "int64";
            case "float":
                return "float";
            case "double":
                return "double";
            case "char":
                return "string";
            default:
                return null;
        }
    }



    static String getClsClassType(PsiType type){
        if(! (type instanceof PsiClassReferenceType)){
            return null;
        }
        PsiClass rType = ((PsiClassReferenceType) type).resolve();
        if(!(rType instanceof ClsClassImpl)){
            return null;
        }

        String typeName = rType.getQualifiedName();
        switch (typeName) {
            case "java.lang.Boolean":
                return "bool";
            case "java.lang.Integer":
                return "int32";
            case "java.lang.Long":
                return "int64";
            case "java.lang.Float":
            case "java.math.BigDecimal":
                return "float";
            case "java.lang.Double":
                return "double";
            case "java.lang.String":
                return "string";
        }
        return null;
    }


    public static class TypeInfo{
        public TypeInfo(String name,List<PsiClass> picClasses){
            setTypeName(name);
            setPicClasses(new ArrayList<>());
            for (PsiClass picClass : picClasses) {
                getPicClasses().add(picClass);
            }
        }

        public TypeInfo(String name,PsiClass... picClasses){
            setTypeName(name);
            setPicClasses(new ArrayList<>());
            for (PsiClass picClass : picClasses) {
                getPicClasses().add(picClass);
            }
        }

        public TypeInfo(String name,PsiType[] fxs,PsiClass... picClasses){
            setTypeName(name);
            setPicClasses(new ArrayList<>());
            for (PsiClass picClass : picClasses) {
                getPicClasses().add(picClass);
            }
            setFxs(fxs);
        }

        String typeName;
        List<PsiClass> picClasses;

        PsiType[] fxs;

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public List<PsiClass> getPicClasses() {
            return picClasses;
        }

        public void setPicClasses(List<PsiClass> picClasses) {
            this.picClasses = picClasses;
        }

        public PsiType[] getFxs() {
            return fxs;
        }

        public void setFxs(PsiType[] fxs) {
            this.fxs = fxs;
        }
    }


}
