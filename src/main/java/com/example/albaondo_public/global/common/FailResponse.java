package com.example.albaondo_public.global.common;

import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FailResponse {

    private final int status;
    private final String code;
    private final String message;
    private final Map<String, String> validation;

    // 기존 호출부의 응답 형식은 유지하면서 영문 오류 코드를 추가한다.
    public FailResponse(int status, String message, Map<String, String> validation) {
        this(
            status,
            switch (status) {
                case 400 -> "INVALID_REQUEST";
                case 401 -> "AUTHENTICATION_REQUIRED";
                case 403 -> "ACCESS_DENIED";
                case 404 -> "RESOURCE_NOT_FOUND";
                case 405 -> "METHOD_NOT_ALLOWED";
                case 409 -> "DATA_CONFLICT";
                default -> "API_ERROR";
            },
            message,
            validation
        );
    }
}
