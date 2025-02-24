package com.gzh;

import com.gzh.annotation.RpcScan;
import com.gzh.config.RpcServiceConfig;
import com.gzh.remoting.transport.netty.server.NettyRpcServer;
import com.gzh.serviceimpl.HelloServiceImpl2;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@RpcScan(basePackage = {"com.gzh"})
public class NettyServerMain {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(NettyServerMain.class);
        NettyRpcServer nettyRpcServer = (NettyRpcServer)context.getBean("nettyRpcServer");
        HelloServiceImpl2 helloServiceImpl2 = new HelloServiceImpl2();
        RpcServiceConfig rpcServerConfig = RpcServiceConfig.builder()
                .version("version2")
                .group("group2")
                .service(helloServiceImpl2).build();
        nettyRpcServer.registerService(rpcServerConfig);
        nettyRpcServer.start();
    }
}
