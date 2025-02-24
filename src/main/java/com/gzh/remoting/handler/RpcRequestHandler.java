package com.gzh.remoting.handler;

import com.gzh.factory.SingletonFactory;
import com.gzh.provider.ServiceProvider;
import com.gzh.provider.ZkServiceProviderImpl;
import com.gzh.remoting.dto.RpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class RpcRequestHandler {

    private final ServiceProvider serviceProvider;

    public RpcRequestHandler() {
        serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    }

    public Object handle(RpcRequest rpcRequest) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Object service = serviceProvider.getService(rpcRequest.getRpcServiceName());//获取服务实例
        return invokeTargetMethod(rpcRequest,service);
    }

    //调用方法
    private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
        Object result = method.invoke(service, rpcRequest.getParameters());//具体服务处理后返回结果
        return result;
    }
}
