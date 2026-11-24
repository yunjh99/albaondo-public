package com.example.albaondo_public.review.repository;

import com.example.albaondo_public.review.domain.ReviewLike;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {
    java.util.Optional<ReviewLike> findByUser_IdAndReview_ReviewId(
        java.util.UUID userId,
        Long reviewId
    );
    long countByReview_ReviewId(Long reviewId);
}
