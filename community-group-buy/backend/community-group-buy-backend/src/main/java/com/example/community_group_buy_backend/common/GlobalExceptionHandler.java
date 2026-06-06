package com.example.community_group_buy_backend.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleBadRequest(IllegalArgumentException ex) {
        return Result.fail(Code.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        return Result.fail(ex.getMessage());
    }
}
