package com.autohome.ah_grpc_plugin.models;

public class MessageFieldInfo {

    public MessageFieldInfo(String name,String type){
        setType(type);
        setName(name);
    }

    public MessageFieldInfo(String name,String type,String jsonName){
        setJsonName(jsonName);
        setName(name);
        setType(type);
    }

    String type;
    String name;
    String jsonName;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJsonName() {
        return jsonName;
    }

    public void setJsonName(String jsonName) {
        this.jsonName = jsonName;
    }
}
