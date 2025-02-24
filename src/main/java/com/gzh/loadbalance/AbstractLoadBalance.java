package com.gzh.loadbalance;

import com.gzh.remoting.dto.RpcRequest;
import com.gzh.utils.CollectionUtils;

import java.util.List;

public abstract class AbstractLoadBalance  implements LoadBalance{
    @Override
    public String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest) {
        if (CollectionUtils.isEmpty(serviceUrlList)) {
            return null;
        }
        if (serviceUrlList.size() == 1) {
            return serviceUrlList.get(0);
        }
        return doSelect(serviceUrlList,rpcRequest);
    }

    protected abstract String doSelect(List<String> serviceAddress,RpcRequest rpcRequest);
}
