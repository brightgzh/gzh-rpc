package com.gzh.proxy;

import com.gzh.config.RpcServiceConfig;
import com.gzh.enums.RpcErrorMessageEnum;
import com.gzh.enums.RpcResponseCodeEnum;
import com.gzh.exception.RpcException;
import com.gzh.remoting.dto.RpcRequest;
import com.gzh.remoting.dto.RpcResponse;
import com.gzh.remoting.transport.RpcRequestTransport;
import com.gzh.remoting.transport.netty.client.NettyRpcClient;
import com.gzh.remoting.transport.socket.SocketRpcClient;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RpcClientProxy implements InvocationHandler {
    private static final String INTERFACE_NAME = "interfaceName";
    private final RpcRequestTransport rpcRequestTranSport;
    private final RpcServiceConfig rpsServiceConfig;

    public RpcClientProxy(RpcRequestTransport rpcRequestTranSport, RpcServiceConfig rpcServiceConfig) {
        this.rpsServiceConfig = rpcServiceConfig;
        this.rpcRequestTranSport = rpcRequestTranSport;
    }

    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(),new Class<?>[] {clazz},this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcRequest rpcRequest = RpcRequest.builder()
                .methodName(method.getName())
                .parameters(args)
                .interfaceName(method.getDeclaringClass().getName())
                .paramTypes(method.getParameterTypes())
                .requestId(UUID.randomUUID().toString())
                .group(rpsServiceConfig.getGroup())
                .version(rpsServiceConfig.getVersion())
                .build();
        RpcResponse<Object> rpcResponse = null;

        if (rpcRequestTranSport instanceof NettyRpcClient) {
            CompletableFuture<RpcResponse<Object>> future = (CompletableFuture<RpcResponse<Object>>) rpcRequestTranSport.sendRpsRequest(rpcRequest);
            rpcResponse = future.get();
        }

        if (rpcRequestTranSport instanceof SocketRpcClient) {
            rpcResponse = (RpcResponse<Object>) ((SocketRpcClient) rpcRequestTranSport).sendRpsRequest(rpcRequest);
        }

        check(rpcResponse,rpcRequest);
        return rpcResponse.getData();
    }

    private void check(RpcResponse<Object> rpcResponse, RpcRequest rpcRequest) {
        if (rpcResponse == null) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (!rpcRequest.getRequestId().equals(rpcResponse.getRequestId())) {
            throw new RpcException(RpcErrorMessageEnum.REQUEST_NOT_MATCH_RESPONSE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (rpcResponse.getCode() == null || !rpcResponse.getCode().equals(RpcResponseCodeEnum.SUCCESS.getCode())) {

            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
    }
}
