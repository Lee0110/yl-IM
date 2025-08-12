package com.lyl.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum OcsErrorCode implements IErrorCode{
    COMMON_ERROR(50000, "通用错误"),
    SYSTEM_ERROR(50001, "系统异常"),
    JSON_PARSE_ERROR(50002, "JSON解析错误"),
    INVALID_DATE_FORMAT(50003, "无效的日期格式：%s"),
    LOCK_ERROR(50004, "获取锁失败，请稍后再试"),
    BEAN_CONVERT_ERROR(50005, "Bean转换错误"),
    GET_USER_ERROR(50006, "获取用户信息异常"),
    API_ERROR(50007, "API调用异常"),
    ;

    private final int code;
    private final String errorMsg;

    public static IErrorCode fromCode(int code) {
        return Arrays.stream(OcsErrorCode.values()).filter(errorCode -> errorCode.getCode() == code).findAny().orElse(null);
    }
}
