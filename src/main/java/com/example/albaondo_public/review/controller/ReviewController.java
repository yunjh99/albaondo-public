package com.example.albaondo_public.review.controller;

import com.example.albaondo_public.global.security.AuthenticatedUser;
import com.example.albaondo_public.review.dto.ReviewWriteRequest;
import com.example.albaondo_public.review.dto.ReviewListResponse;
import com.example.albaondo_public.review.service.ReviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestHeader("Idempotency-Key") UUID requestId,
        @Valid @RequestBody ReviewWriteRequest request
    ) {
        reviewService.create(user.userId(), requestId, request);
    }

    @GetMapping
    public ReviewListResponse list(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return reviewService.list(user.userId(), page, size);
    }

    @GetMapping("/mine")
    public ReviewListResponse listMine(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return reviewService.listMine(user.userId(), page, size);
    }

    @PutMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long reviewId,
        @Valid @RequestBody ReviewWriteRequest request
    ) {
        reviewService.update(user.userId(), reviewId, request);
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long reviewId
    ) {
        reviewService.delete(user.userId(), reviewId);
    }
}
