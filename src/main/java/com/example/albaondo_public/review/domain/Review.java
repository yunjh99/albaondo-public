package com.example.albaondo_public.review.domain;

import com.example.albaondo_public.global.error.ErrorCode;
import com.example.albaondo_public.global.error.exception.ApiException;
import com.example.albaondo_public.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@DynamicUpdate
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(
    name = "uk_reviews_author_request", columnNames = {"author_user_id", "request_id"}
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    // 키 도입 전의 후기는 두 값이 모두 null일 수 있다.
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "request_id", columnDefinition = "BINARY(16)", updatable = false)
    private UUID requestId;

    @Column(name = "request_hash", length = 64, updatable = false)
    private String requestHash;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "work_year")
    private Short workYear;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_user_id", nullable = false, columnDefinition = "BINARY(16)")
    private User author;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PUBLISHED;

    public Review(User author, String title, String content, Short workYear) {
        this.author = Objects.requireNonNull(author, "작성자는 필수입니다.");
        updateContent(title, content, workYear);
    }

    public void updateContent(String title, String content, Short workYear) {
        if (status != ReviewStatus.PUBLISHED) {
            throw new ApiException(ErrorCode.REVIEW_NOT_EDITABLE);
        }
        Objects.requireNonNull(title, "제목은 필수입니다.");
        Objects.requireNonNull(content, "내용은 필수입니다.");
        Objects.requireNonNull(workYear, "근무 연도는 필수입니다.");
        this.title = title;
        this.content = content;
        this.workYear = workYear;
    }

    public void identifyCreationRequest(UUID key, String hash) {
        if (requestId != null) {
            throw new IllegalStateException("등록 요청 키는 변경할 수 없습니다.");
        }
        Objects.requireNonNull(hash);
        this.requestId = Objects.requireNonNull(key);
        this.requestHash = hash;
    }

    public void hide() {
        if (status == ReviewStatus.PUBLISHED) {
            status = ReviewStatus.HIDDEN;
        }
    }

    public boolean isWrittenBy(UUID userId) {
        return userId != null && userId.equals(author.getId());
    }

    void requireOtherAuthor(User user) {
        Objects.requireNonNull(user, "회원은 필수입니다.");
        if (user == author || isWrittenBy(user.getId())) {
            throw new ApiException(ErrorCode.SELF_REACTION_NOT_ALLOWED);
        }
    }
}
