package com.example.albaondo_public.review.repository;

import com.example.albaondo_public.review.domain.ReviewHoneyVote;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewHoneyVoteRepository extends JpaRepository<ReviewHoneyVote, Long> {
    Optional<ReviewHoneyVote> findByUser_IdAndReview_ReviewId(UUID userId, Long reviewId);
    long countByReview_ReviewIdAndHoneyJobTrue(Long reviewId);
}
