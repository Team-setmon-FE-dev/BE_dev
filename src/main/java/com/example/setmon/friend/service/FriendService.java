package com.example.setmon.friend.service;

import com.example.setmon.friend.entity.FriendshipStatus;
import com.example.setmon.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendService {

    private final FriendshipRepository friendshipRepository;

    public boolean areFriends(Long userId, Long otherId) {
        return friendshipRepository.existsBetweenWithStatus(userId, otherId, FriendshipStatus.ACCEPTED);
    }

    // TODO(3주차): 코드로 요청, 받은 요청 목록, 수락, 거절, 친구 목록, 친구 삭제
}
