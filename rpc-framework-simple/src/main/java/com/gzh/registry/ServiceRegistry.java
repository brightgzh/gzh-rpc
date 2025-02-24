package com.gzh.registry;

import com.gzh.extension.SPI;

import java.net.InetSocketAddress;

@SPI
public interface ServiceRegistry {
    void registerService(String rpcServiceName,InetSocketAddress inetSocketAddress);
}
