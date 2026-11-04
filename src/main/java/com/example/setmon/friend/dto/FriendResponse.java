package com.example.setmon.friend.dto;

import java.time.Instant;

public record FriendResponse(Long userId, String nickname, Instant friendsSince) {
}
