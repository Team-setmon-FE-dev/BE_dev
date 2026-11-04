package com.example.setmon.common.security;

import com.example.setmon.common.exception.BusinessException;
import com.example.setmon.common.exception.ErrorCode;
import com.example.setmon.friend.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 조회는 본인 또는 수락된 친구, 쓰기는 본인만. */
@Component
@RequiredArgsConstructor
public class AccessGuard {

    private final FriendService friendService;

    public void checkCanView(Long viewerId, Long ownerId) {
        if (viewerId.equals(ownerId)) {
            return;
        }
        if (!friendService.areFriends(viewerId, ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    public void checkOwner(Long viewerId, Long ownerId) {
        if (!viewerId.equals(ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
