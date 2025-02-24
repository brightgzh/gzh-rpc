package com.gzh.loadbalance.loadbalancer;


import com.gzh.loadbalance.AbstractLoadBalance;
import com.gzh.remoting.dto.RpcRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class ConsistentHashLoadBalance extends AbstractLoadBalance {
    private final ConcurrentHashMap<String,ConsistentHashSelector> selectors = new ConcurrentHashMap<>();
    @Override
    protected String doSelect(List<String> serviceAddress, RpcRequest rpcRequest) {
        int identify = System.identityHashCode(serviceAddress);
        String rpcServiceName = rpcRequest.getRpcServiceName();
        ConsistentHashSelector selector = selectors.get(rpcServiceName);
        if (selector == null || selector.identifyHashCode != identify) {
            selectors.put(rpcServiceName,new ConsistentHashSelector(serviceAddress,160,identify));
            selector = selectors.get(rpcServiceName);
        }
        return selector.select(rpcServiceName + Arrays.stream(rpcRequest.getParameters()));
    }

    static class ConsistentHashSelector {
        private final TreeMap<Long,String> virtualInvokers;
        private final int identifyHashCode;

        ConsistentHashSelector(List<String> invokers,int replicaNumber,int identifyHashCode) {
            this.virtualInvokers = new TreeMap<>();
            this.identifyHashCode = identifyHashCode;

            for (String invoker:invokers) {
                for (int i = 0;i < replicaNumber / 4;i++) {
                    byte[] digest = md5(invoker+i);
                    for (int h = 0;h < 4;h++) {
                        long m = hash(digest,h);
                        virtualInvokers.put(m,invoker);
                    }
                }
            }

        }

        static byte[] md5(String key) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            return md.digest();
        }

        static long hash(byte[] digest,int idx) {
            return ((long) (digest[3 + idx * 4] & 255) << 24 | (long) (digest[2 + idx * 4] & 255) << 16 | (long) (digest[1 + idx * 4] & 255) << 8 | (long) (digest[idx * 4] & 255)) & 4294967295L;
        }

        public String select(String rpcServiceKey) {
            byte[] digest = md5(rpcServiceKey);
            return selectForKey(hash(digest,0));
        }

        public String selectForKey(long hashCode) {
            // 1. 使用 tailMap 查找大于等于 hashCode 的第一个节点
            Map.Entry<Long, String> entry = virtualInvokers.tailMap(hashCode, true).firstEntry();

            // 2. 如果没有找到匹配项，则返回虚拟节点的第一个节点（循环回去）
            if (entry == null) {
                entry = virtualInvokers.firstEntry();
            }

            // 3. 返回服务地址（invoker）
            return entry.getValue();
        }
    }
}
