package com.example.albaondo_public.review.dto;

import com.example.albaondo_public.review.domain.Review;
import java.util.UUID;

public record ReviewListItem(
    Long reviewId,
    String title,
    String content,
    Short workYear,
    Long likeCount,
    boolean mine
) {
    static ReviewListItem from(Review review, UUID viewerId) {
        return new ReviewListItem(
            review.getReviewId(), review.getTitle(), review.getContent(),
            review.getWorkYear(), review.getLikeCount(), review.isWrittenBy(viewerId)
        );
    }
}
