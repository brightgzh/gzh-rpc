package com.gzh.remoting.transport.netty.client;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelProvider {

    private final Map<String, Channel> channelMap;

    public ChannelProvider() {
        channelMap = new ConcurrentHashMap<>();
    }

    //获取channel
    public Channel get(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        if (channelMap.containsKey(key)) {
            Channel channel = channelMap.get(key);
            //如果channel状态正常
            if (channel != null && channel.isActive()) {
                return channel;
            }
            channelMap.remove(key);
        }
        return null;
    }
    //添加通道
    public void set(InetSocketAddress inetSocketAddress,Channel channel) {
        String key = inetSocketAddress.toString();
        channelMap.put(key,channel);
    }
}
