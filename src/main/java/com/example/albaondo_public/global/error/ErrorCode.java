package com.example.albaondo_public.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인 후 이용해 주세요."),
    MEMBER_NOT_ACTIVE(HttpStatus.FORBIDDEN, "현재 계정 상태에서는 이용할 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),

    INVALID_REVIEW(HttpStatus.BAD_REQUEST, "후기 입력값을 확인해 주세요."),
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "같은 요청 키로 다른 내용을 등록할 수 없습니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "후기를 찾을 수 없습니다."),
    REVIEW_NOT_OWNED(HttpStatus.FORBIDDEN, "본인이 작성한 후기만 수정하거나 삭제할 수 있습니다."),
    REVIEW_NOT_EDITABLE(HttpStatus.CONFLICT, "현재 상태에서는 후기를 수정할 수 없습니다."),
    SELF_REACTION_NOT_ALLOWED(HttpStatus.FORBIDDEN, "본인 후기에는 반응할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
