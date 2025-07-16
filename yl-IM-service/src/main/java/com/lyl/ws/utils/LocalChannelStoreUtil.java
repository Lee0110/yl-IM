package com.lyl.ws.utils;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocalChannelStoreUtil {
    private static final ConcurrentHashMap<Long, Channel> userChannel = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Channel, Long> channelUser = new ConcurrentHashMap<>();

    public void addChannel(Long userId, Channel channel) {
        userChannel.put(userId, channel);
        channelUser.put(channel, userId);
    }

    public Channel getChannelByUserId(Long userId) {
        return userChannel.get(userId);
    }

    public Long getUserIdByChannel(Channel channel) {
        return channelUser.get(channel);
    }

    public void removeChannel(Long userId) {
        Channel channel = userChannel.remove(userId);
        if (channel != null) {
            channelUser.remove(channel);
        }
    }

    public Set<Long> getAllUserIds() {
        return userChannel.keySet();
    }
}
