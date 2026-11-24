package com.example.albaondo_public.review.service;

import com.example.albaondo_public.global.error.ErrorCode;
import com.example.albaondo_public.global.error.exception.ApiException;
import com.example.albaondo_public.review.domain.Review;
import com.example.albaondo_public.review.domain.ReviewStatus;
import com.example.albaondo_public.review.dto.ReviewWriteRequest;
import com.example.albaondo_public.review.dto.ReviewListResponse;
import com.example.albaondo_public.review.repository.ReviewRepository;
import com.example.albaondo_public.user.domain.User;
import com.example.albaondo_public.user.service.ActiveUserProvider;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ActiveUserProvider activeUsers;

    @Transactional
    public void create(UUID authorId, UUID requestId, ReviewWriteRequest request) {
        User author = activeUsers.requireActiveForUpdate(authorId);
        String fingerprint = request.creationFingerprint();
        var existing = reviewRepository.findByAuthor_IdAndRequestId(authorId, requestId);
        if (existing.isPresent()) {
            if (!fingerprint.equals(existing.get().getRequestHash())) {
                throw new ApiException(ErrorCode.IDEMPOTENCY_CONFLICT);
            }
            return;
        }
        Review review = new Review(
                author,
                request.title(),
                request.content(),
                request.workYear());
        review.identifyCreationRequest(requestId, fingerprint);
        reviewRepository.save(review);
    }

    @Transactional
    public void update(UUID authorId, Long reviewId, ReviewWriteRequest request) {
        Review review = requireOwnedReviewForUpdate(authorId, reviewId);
        review.updateContent(
                request.title(),
                request.content(),
                request.workYear()
        );
    }

    @Transactional
    public void delete(UUID authorId, Long reviewId) {
        requireOwnedReviewForUpdate(authorId, reviewId).hide();
    }

    @Transactional(readOnly = true)
    public ReviewListResponse list(UUID viewerId, int page, int size) {
        activeUsers.requireActive(viewerId);
        Slice<Review> reviews = reviewRepository.findByStatus(ReviewStatus.PUBLISHED, pageRequest(page, size));
        return ReviewListResponse.from(reviews, viewerId);
    }

    @Transactional(readOnly = true)
    public ReviewListResponse listMine(UUID authorId, int page, int size) {
        activeUsers.requireActive(authorId);
        Slice<Review> reviews = reviewRepository.findByAuthor_IdAndStatus(
            authorId, ReviewStatus.PUBLISHED, pageRequest(page, size)
        );
        return ReviewListResponse.from(reviews, authorId);
    }

    private Review requireOwnedReviewForUpdate(UUID authorId, Long reviewId) {
        activeUsers.requireActiveForUpdate(authorId);
        Review review = reviewRepository.findForUpdate(reviewId)
            .orElseThrow(() -> new ApiException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.isWrittenBy(authorId)) {
            throw new ApiException(ErrorCode.REVIEW_NOT_OWNED);
        }
        return review;
    }

    private PageRequest pageRequest(int page, int size) {
        if (size < 1 || size > 50) {
            throw new ApiException(ErrorCode.INVALID_REVIEW, Map.of("size", "조회 개수는 1~50으로 입력해 주세요."));
        }
        if (page < 0 || (long) page * size > Integer.MAX_VALUE) {
            throw new ApiException(ErrorCode.INVALID_REVIEW, Map.of("page", "조회 가능한 페이지 번호를 입력해 주세요."));
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reviewId"));
    }
}
