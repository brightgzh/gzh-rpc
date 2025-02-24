package com.gzh.exception;

import com.gzh.enums.RpcErrorMessageEnum;

public class RpcException extends RuntimeException{

    public RpcException(RpcErrorMessageEnum messageEnum,String message) {
        super(messageEnum+":"+message);
    }

    public RpcException(RpcErrorMessageEnum messageEnum) {
        super(messageEnum.getMessage());
    }

    public RpcException(String message,Throwable cause) {
        super(message,cause);
    }
}
