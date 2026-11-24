package com.example.albaondo_public.review.dto;

import com.example.albaondo_public.review.domain.Review;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Slice;

public record ReviewListResponse(
    List<ReviewListItem> items,
    int page,
    int size,
    boolean hasNext
) {
    public static ReviewListResponse from(Slice<Review> reviews, UUID viewerId) {
        List<ReviewListItem> items = reviews.map(review -> ReviewListItem.from(review, viewerId)).getContent();
        return new ReviewListResponse(items, reviews.getNumber(), reviews.getSize(), reviews.hasNext());
    }
}
