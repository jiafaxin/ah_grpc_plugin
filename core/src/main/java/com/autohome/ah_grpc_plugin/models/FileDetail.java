package com.autohome.ah_grpc_plugin.models;

import org.apache.commons.lang3.StringUtils;

public class FileDetail {
    String message;
    String content;

    public boolean notExists(){
        return (message!=null && message.equals("404 File Not Found")) || content==null;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
