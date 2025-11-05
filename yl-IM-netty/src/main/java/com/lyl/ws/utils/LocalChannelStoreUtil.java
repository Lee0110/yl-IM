package com.lyl.ws.utils;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocalChannelStoreUtil {
    private static final ConcurrentHashMap<Long, Channel> userChannel = new ConcurrentHashMap<>();

    public void addChannel(Long userId, Channel channel) {
        userChannel.put(userId, channel);
    }

    public Channel getChannelByUserId(Long userId) {
        return userChannel.get(userId);
    }

    public void removeChannel(Long userId) {
        userChannel.remove(userId);
    }

    public Set<Long> getAllUserIds() {
        return userChannel.keySet();
    }
}
