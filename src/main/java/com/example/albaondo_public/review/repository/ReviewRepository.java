package com.example.albaondo_public.review.repository;

import com.example.albaondo_public.review.domain.Review;
import com.example.albaondo_public.review.domain.ReviewStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByAuthor_IdAndRequestId(UUID authorId, UUID requestId);

    Slice<Review> findByStatus(ReviewStatus status, Pageable pageable);

    Slice<Review> findByAuthor_IdAndStatus(UUID authorId, ReviewStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE
    )
    @org.springframework.data.jpa.repository.Query("select r from Review r where r.reviewId = :id")
    java.util.Optional<Review> findForUpdate(Long id);
}
