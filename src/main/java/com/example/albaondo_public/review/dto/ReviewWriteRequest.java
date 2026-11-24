package com.example.albaondo_public.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.hibernate.validator.constraints.CodePointLength;

public record ReviewWriteRequest(
    @NotBlank(message = "제목을 입력해 주세요.")
    @CodePointLength(max = 100, message = "제목은 100자 이하로 입력해 주세요.")
    String title,
    @NotBlank(message = "내용을 입력해 주세요.")
    String content,
    @NotNull(message = "근무 연도를 입력해 주세요.")
    Short workYear
) {
    public String creationFingerprint() {
        // 길이를 함께 넣어 제목과 본문에 구분 문자가 있어도 각 필드의 경계를 보존한다.
        String payload = title.length() + ":" + title + content.length() + ":" + content + workYear;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
