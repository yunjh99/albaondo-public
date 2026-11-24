package com.example.albaondo_public.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
}
