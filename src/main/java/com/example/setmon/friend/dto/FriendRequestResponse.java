package com.example.setmon.friend.dto;

import java.time.Instant;

/** 받은 친구 요청 */
public record FriendRequestResponse(Long requestId, Long userId, String nickname, Instant requestedAt) {
}
