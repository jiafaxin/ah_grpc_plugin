package com.autohome.ah_grpc_plugin.utils;

/**
 * proto文件命名工具类
 */
public class ProtoNameUtils {

    public static String getNameOnly(String name){
        return name.substring(0,name.indexOf("."));
    }

    public static String javaMethodName(String name){
        return name.substring(0,1).toLowerCase().concat(name.substring(1));
    }

    public static String inPackage(String name){
        return name.replace("_","");
    }

    public static String goName(String name){
        return name.replace("_","");
    }

    public static String toPascal(String name){
        String[] parts = name.split("_");
        String pascalString = "";
        for (int i = 0; i < parts.length; i++){
            String string = parts[i];
            String temp = string.substring(0, 1).toUpperCase()+string.substring(1);
            pascalString = pascalString.concat(temp);
        }
        return pascalString;
    }



    public static String getBaseName(String key, boolean isRepeated){
        StringBuilder result = new StringBuilder();
        boolean nu = true;
        for (char c : key.toCharArray()) {
            if(c == '_'){
                nu = true;
                continue;
            }
            String v = String.valueOf(c);
            if(nu){
                v = v.toUpperCase();
            }
            result.append(v);
            if(nu){
                nu = false;
            }
        }
        String name = result.toString();
        if(isRepeated && name.charAt(name.length()-1) == 's'){
            name = name.substring(0,name.length()-1);
        }
        return name;
    }

    public static String getFieldName(String name){
        boolean isFirst = true;
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (isFirst) {
                isFirst = false;
                sb.append(String.valueOf(c).toLowerCase());
                continue;
            }
            if (Character.isLowerCase(c) || (Character.isDefined(c) && !Character.isUpperCase(c)) || c == '_') {
                sb.append(c);
                continue;
            }
            if(Character.isUpperCase(c)){
                sb.append("_" + String.valueOf(c).toLowerCase());
                continue;
            }
        }
        return sb.toString();
    }

}
