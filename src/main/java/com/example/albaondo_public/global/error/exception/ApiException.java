package com.example.albaondo_public.global.error.exception;

import com.example.albaondo_public.global.error.ErrorCode;
import java.util.Map;
import lombok.Getter;

@Getter
public final class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, String> validation;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, Map.of());
    }

    public ApiException(ErrorCode errorCode, Map<String, String> validation) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.validation = Map.copyOf(validation);
    }
}
