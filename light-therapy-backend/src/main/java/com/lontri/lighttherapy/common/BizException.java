package com.lontri.lighttherapy.common;

import org.springframework.http.HttpStatus;

public class BizException extends RuntimeException {
    private final int code;
    private final HttpStatus httpStatus;

    public BizException(int code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }

    public BizException(int code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public int getCode() { return code; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}
