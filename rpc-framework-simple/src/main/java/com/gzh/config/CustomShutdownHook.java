package com.gzh.config;

import com.gzh.registry.zk.util.CuratorUtils;
import com.gzh.remoting.transport.netty.server.NettyRpcServer;
import com.gzh.utils.concurrent.threadpool.ThreadPoolFactoryUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

@Slf4j
public class CustomShutdownHook {
    private static final CustomShutdownHook CUSTOM_SHUTDOWN_HOOK = new CustomShutdownHook();
    public static CustomShutdownHook getCustomShutdownHook() {
        return CUSTOM_SHUTDOWN_HOOK;
    }

    //清空zk注册节点，线程
    public void clearAll() {
        log.info("addShutdownHook for clearAll");
        Runtime.getRuntime().addShutdownHook(new Thread(()-> {//周期钩子，JVM停止时执行
            try {
                InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), NettyRpcServer.PORT);
                CuratorUtils.cleatRegistry(CuratorUtils.getZkClient(),inetSocketAddress);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            ThreadPoolFactoryUtil.shutdownAllThreadPool();
        }));
    }
}
