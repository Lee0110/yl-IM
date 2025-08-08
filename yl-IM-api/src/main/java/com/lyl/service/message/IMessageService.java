package com.lyl.service.message;

import com.lyl.domain.dto.MessageDTO;

public interface IMessageService {
    /**
     * 发送消息给指定用户（本机 -> 一致性哈希选择服务器 -> 广播）
     *
     * @param messageDTO 消息内容
     */
    void sendMessageToUser(MessageDTO messageDTO);

    /**
     * 发送消息到选定的服务器，通过一致性哈希算法选择服务器
     *
     * @param messageDTO 消息内容
     * @return 是否发送成功
     */
    boolean sendMessageToSelectedServer(MessageDTO messageDTO);

    /**
     * 通过HTTP调用远程服务发送消息
     *
     * @param serverIpPort 目标服务器ID (IP:端口)
     * @param messageDTO 消息内容
     * @return 是否发送成功
     */
    boolean sendMessageToRemoteServer(String serverIpPort, MessageDTO messageDTO);

    /**
     * 广播消息给所有服务实例
     *
     * @param messageDTO 消息内容
     */
    void broadcastMessage(MessageDTO messageDTO);

    /**
     * 发送消息到本机连接的用户
     *
     * @param messageDTO 消息内容
     */
    boolean sendMessageToLocalUser(MessageDTO messageDTO);
}
