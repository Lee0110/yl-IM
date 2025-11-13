package com.lyl.enums;

import com.lyl.exception.IMException;

import java.util.Map;
import java.util.Objects;

public interface IHandlerKey {
    String getHandlerKey();

    static <T> T getHandler(IHandlerKey handlerKey, Map<String, T> handlerMap) {
        T t = handlerMap.get(handlerKey.getHandlerKey());
        if (Objects.isNull(t)) {
            throw new IMException("未找到对应的处理器, handlerKey=" + handlerKey.getHandlerKey() + ", " + "handlerMap=" + handlerMap);
        }
        return t;
    }
}
