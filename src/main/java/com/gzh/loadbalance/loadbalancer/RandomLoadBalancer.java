package com.gzh.loadbalance.loadbalancer;

import com.gzh.loadbalance.AbstractLoadBalance;
import com.gzh.remoting.dto.RpcRequest;

import java.util.List;
import java.util.Random;

public class RandomLoadBalancer extends AbstractLoadBalance {
    @Override
    protected String doSelect(List<String> serviceAddress, RpcRequest rpcRequest) {
        Random random = new Random();
        return serviceAddress.get(random.nextInt(serviceAddress.size()));
    }
}
