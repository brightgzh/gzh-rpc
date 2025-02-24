package com.gzh.registry.zk;

import com.gzh.enums.LoadBalanceEnum;
import com.gzh.enums.RpcErrorMessageEnum;
import com.gzh.exception.RpcException;
import com.gzh.extension.ExtensionLoader;
import com.gzh.extension.SPI;
import com.gzh.loadbalance.LoadBalance;
import com.gzh.registry.ServiceDiscovery;
import com.gzh.registry.zk.util.CuratorUtils;
import com.gzh.remoting.dto.RpcRequest;
import com.gzh.utils.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {
    private final LoadBalance loadBalance;

    public ZkServiceDiscoveryImpl() {
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(LoadBalanceEnum.LOAD_BALANCE.getName());
    }

    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        String rpcServiceName = rpcRequest.getRpcServiceName();
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        List<String> childrenNodes = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
        if (CollectionUtils.isEmpty(childrenNodes)) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }

        String serviceAddress = loadBalance.selectServiceAddress(childrenNodes, rpcRequest);
        log.info("success to find the serviceAddress: [{}]",serviceAddress);
        String[] split = serviceAddress.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(host,port);
    }
}
