package com.autohome.ah_grpc_plugin.models;

import com.autohome.ah_grpc_plugin.enums.ResultCode;

public class MethodResult {

    ResultCode code;

    String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess(){
        return getCode().equals(ResultCode.SUCCESS);
    }


    public static MethodResult success(){
        return new MethodResult(){{setCode(ResultCode.SUCCESS);}};
    }

    public static MethodResult success(String _message) {
        return new MethodResult() {{
            setCode(ResultCode.SUCCESS);
            setMessage(_message);
        }};
    }

    public static MethodResult fail(){
        return new MethodResult(){{setCode(ResultCode.ERROR);}};
    }
    public static MethodResult fail(ResultCode resultCode){
        return new MethodResult(){{setCode(resultCode);}};
    }
    public static MethodResult fail(ResultCode resultCode,String message){
        return new MethodResult(){{setCode(resultCode);setMessage(message);}};
    }



    public ResultCode getCode() {
        return code;
    }

    public void setCode(ResultCode code) {
        this.code = code;
    }
}
