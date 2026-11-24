package com.example.albaondo_public;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.albaondo_public.review.domain.Review;
import com.example.albaondo_public.global.error.ErrorCode;
import com.example.albaondo_public.global.error.exception.ApiException;
import com.example.albaondo_public.review.domain.ReviewHoneyVote;
import com.example.albaondo_public.review.domain.ReviewLike;
import com.example.albaondo_public.user.domain.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReviewReactionTest {

    @Test
    void sameMemberCannotReactEvenWithAnotherEntityInstance() {
        UUID authorId = UUID.randomUUID();
        Review review = new Review(member(authorId), "주말 근무 후기", "오픈 준비를 맡았습니다.", (short) 2025);
        User reader = member(authorId);

        assertEquals(ErrorCode.SELF_REACTION_NOT_ALLOWED,
            assertThrows(ApiException.class, () -> new ReviewLike(reader, review)).getErrorCode());
        assertThrows(ApiException.class, () -> new ReviewHoneyVote(reader, review, true));
    }

    @Test
    void otherMemberCanUseBothReactions() {
        Review review = new Review(member(UUID.randomUUID()), "주말 근무 후기", "오픈 준비를 맡았습니다.", (short) 2025);
        User reader = member(UUID.randomUUID());

        assertSame(review, new ReviewLike(reader, review).getReview());
        ReviewHoneyVote vote = new ReviewHoneyVote(reader, review, false);
        vote.changeVote(true);
        assertTrue(vote.isHoneyJob());
    }

    private User member(UUID id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }
}
