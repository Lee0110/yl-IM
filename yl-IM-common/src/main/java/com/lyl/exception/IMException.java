package com.lyl.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class IMException extends RuntimeException {
    private final IErrorCode errorCode;

    public IMException(String message) {
        super(message);
        this.errorCode = IMErrorCode.COMMON_ERROR;
    }


    public IMException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = IMErrorCode.COMMON_ERROR;
    }

    public IMException(IErrorCode errorCode) {
        super(errorCode.getErrorMsg());
        this.errorCode = errorCode;
    }

    public IMException(IErrorCode errorCode, Throwable cause) {
        super(errorCode.getErrorMsg(), cause);
        this.errorCode = errorCode;
    }

    public IMException(String message, IErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public IMException(IErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getErrorMsg(), args));
        this.errorCode = errorCode;
    }

    public IMException(IErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
