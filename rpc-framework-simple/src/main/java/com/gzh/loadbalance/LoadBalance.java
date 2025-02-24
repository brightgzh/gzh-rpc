package com.gzh.loadbalance;

import com.gzh.extension.SPI;
import com.gzh.remoting.dto.RpcRequest;

import java.util.List;

@SPI
public interface LoadBalance {
    String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest);
}
