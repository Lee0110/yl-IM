package com.lyl.utils;

import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * MDC上下文工具类，用于在多线程环境中传递MDC上下文
 *
 * @author weicai
 */
public class MDCContextUtil {

    /**
     * traceId在MDC中的key
     */
    public static final String TRACE_ID_KEY = "TRACE_ID";

    /**
     * 获取当前的traceId，如果不存在则创建一个新的
     */
    public static String getTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
            MDC.put(TRACE_ID_KEY, traceId);
        }
        return traceId;
    }

    /**
     * 生成一个新的traceId
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 设置traceId
     */
    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
    }

    /**
     * 清除traceId
     */
    public static void clearTraceId() {
        MDC.remove(TRACE_ID_KEY);
    }

    /**
     * 获取当前线程的MDC上下文
     */
    public static Map<String, String> getCopyOfContextMap() {
        return MDC.getCopyOfContextMap();
    }

    /**
     * 设置MDC上下文
     */
    public static void setContextMap(Map<String, String> contextMap) {
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
    }

    /**
     * 装饰Runnable，使其能够传递MDC上下文
     */
    public static Runnable wrap(final Runnable runnable) {
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previousContextMap = MDC.getCopyOfContextMap();
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                if (previousContextMap != null) {
                    MDC.setContextMap(previousContextMap);
                } else {
                    MDC.clear();
                }
            }
        };
    }

    /**
     * 装饰Callable，使其能够传递MDC上下文
     */
    public static <T> Callable<T> wrap(final Callable<T> callable) {
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previousContextMap = MDC.getCopyOfContextMap();
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                return callable.call();
            } finally {
                if (previousContextMap != null) {
                    MDC.setContextMap(previousContextMap);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
