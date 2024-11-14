package com.autohome.ah_grpc_plugin.services;


import com.autohome.ah_grpc_plugin.models.MessageFieldInfo;
import com.autohome.ah_grpc_plugin.utils.ProtoNameUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class JsonToMessage {

    String name;

    JSONObject o;

    List<MessageFieldInfo> fields = new ArrayList<>();



    List<JsonToMessage> objects = new ArrayList<>();

    public JsonToMessage(String name, String json){
        this.name = name;
        o = new JSONObject(json);

        fields.add(new MessageFieldInfo("return_code","int32","returncode"));
        fields.add(new MessageFieldInfo("return_msg","string","message"));

        buildFiedls();
    }

    public JsonToMessage(String name,JSONObject jsonObject){
        this.name = name;
        o = jsonObject;
        buildFiedls();
    }

    void buildFiedls(){
        for (String key : o.keySet()) {
            Object value = o.get(key);
            if(value instanceof JSONObject){
                String name = ProtoNameUtils.getBaseName(key,false);
                fields.add(new MessageFieldInfo(key, name));
                objects.add(new JsonToMessage(name,(JSONObject) value));
            }else if(value instanceof JSONArray){
                JSONArray jaValue = (JSONArray) value;
                if(jaValue.length() == 0){
                    fields.add(new MessageFieldInfo(key,"repeated string"));
                }else{
                    Object jaItem = jaValue.get(0);
                    if(jaItem instanceof JSONArray){
                        //无法识别的字段
                    }else if(jaItem instanceof JSONObject){
                        String name = ProtoNameUtils.getBaseName(key,true);
                        fields.add(new MessageFieldInfo(key,"repeated " + name));
                        objects.add(new JsonToMessage(name,(JSONObject)jaItem));
                    }else{
                        fields.add(new MessageFieldInfo(key,"repeated "+getValueType(jaItem)));
                    }
                }
            }else{
                if(key.equals("returncode") || key.equals("return_code")){
                    continue;
                }
                if(key.equals("message") || key.equals("return_msg")){
                    continue;
                }
                fields.add(new MessageFieldInfo(key,getValueType(value)));
            }
        }
    }


    String getValueType(Object value){
        if(value instanceof Integer){
            return "int32";
        }else if(value instanceof Boolean){
            return "bool";
        }else if(value instanceof String){
            return "string";
        }
        return "string";
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
        fields.forEach(item -> {
            String k = item.getName();
            String v = item.getType();
            for (int i1 = 0; i1 < finalDeep; i1++) {
                sb.append(" ");
            }
            String fieldName = ProtoNameUtils.getFieldName(k);

            sb.append("  " + v + " " + fieldName + " = " + (i.getAndIncrement()) + "");

            if(StringUtils.isNotBlank(item.getJsonName())){
                sb.append(" [json_name = \"" + item.getJsonName() + "\"]");
            } else if (!fieldName.equals(k)) {
                sb.append(" [json_name = \"" + k + "\"]");
            }

            sb.append(";\n");
        });

        for (JsonToMessage object : objects) {
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
}
