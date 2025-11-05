package com.lyl.ws.constant;

import io.netty.util.AttributeKey;

import java.util.concurrent.atomic.AtomicInteger;

public class ChannelAttributeKeyConstant {

    public static final AttributeKey<Long> USER_ID_KEY = AttributeKey.valueOf("USER_ID_KEY");

    public static final AttributeKey<AtomicInteger> HEART_BEAT_TIMES = AttributeKey.valueOf("HEART_BEAT_TIMES");
}
