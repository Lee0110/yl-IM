package com.lyl.domain;

import lombok.Data;

@Data
public class Result<T> {
    private int code;

    private String msg;

    private T data;

    public static Result<?> success() {
        Result<?> result = new Result<>();
        result.setCode(200);
        result.setMsg("Success");
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("Success");
        result.setData(data);
        return result;
    }

    public static Result<?> error(int code, String msg) {
        Result<?> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

    public static Result<?> error(String msg) {
        return error(500, msg);
    }

    public static Result<?> error() {
        return error(500, "Internal Server Error");
    }
}
