package com.example.albaondo_public.user.service;

import com.example.albaondo_public.global.error.ErrorCode;
import com.example.albaondo_public.global.error.exception.ApiException;
import com.example.albaondo_public.user.domain.User;
import com.example.albaondo_public.user.domain.UserStatus;
import com.example.albaondo_public.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 서비스에서 회원 조회·자격 확인을 재사용한다. 게시물 소유권 검사는 각 도메인이 담당한다. */
@Component
@RequiredArgsConstructor
public class ActiveUserProvider {

    private final UserRepository userRepository;

    /** 저장 트랜잭션 동안 같은 사용자의 후기 등록을 직렬화한다. */
    @Transactional
    public User requireActiveForUpdate(UUID userId) {
        if (userId == null) throw new ApiException(ErrorCode.AUTHENTICATION_REQUIRED);
        User user = userRepository.findForUpdate(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null) {
            throw new ApiException(ErrorCode.MEMBER_NOT_ACTIVE);
        }
        return user;
    }

    // 호출 서비스의 트랜잭션이 있으면 참여한다. ID는 인증된 사용자 정보에서 전달해야 한다.
    @Transactional(readOnly = true)
    public User requireActive(UUID userId) {
        if (userId == null) throw new ApiException(ErrorCode.AUTHENTICATION_REQUIRED);
        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null) {
            throw new ApiException(ErrorCode.MEMBER_NOT_ACTIVE);
        }
        return user;
    }
}
