package com.lyl.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息类型枚举
 * 定义不同类型的消息及其对应的处理器
 */
@Getter
@AllArgsConstructor
public enum MessageTypeEnum implements IBaseEnum<Integer>, IHandlerKey {
    /**
     * 普通消息
     */
    TEXT(1, "文本消息", "textMessageHandler"),

    /**
     * 系统消息
     */
    SYSTEM(2, "系统消息", "systemMessageHandler"),

    /**
     * 图片消息
     */
    PIC(3, "图片消息", "picMessageHandler"),

    /**
     * 视频消息
     */
    VIDEO(4, "视频消息", "videoMessageHandler"),

    /**
     * 文件消息
     */
    FILE(5, "文件消息", "fileMessageHandler");

    private final Integer value;
    private final String desc;
    private final String handlerKey;
}