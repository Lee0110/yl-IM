package com.lyl.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OcsException extends RuntimeException {
    private final IErrorCode errorCode;

    public OcsException(String message) {
        super(message);
        this.errorCode = OcsErrorCode.COMMON_ERROR;
    }


    public OcsException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = OcsErrorCode.COMMON_ERROR;
    }

    public OcsException(IErrorCode errorCode) {
        super(errorCode.getErrorMsg());
        this.errorCode = errorCode;
    }

    public OcsException(IErrorCode errorCode, Throwable cause) {
        super(errorCode.getErrorMsg(), cause);
        this.errorCode = errorCode;
    }

    public OcsException(String message, IErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public OcsException(IErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getErrorMsg(), args));
        this.errorCode = errorCode;
    }

    public OcsException(IErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
