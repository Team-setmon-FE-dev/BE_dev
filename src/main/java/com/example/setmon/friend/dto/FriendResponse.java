package com.example.setmon.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record FriendResponse(Long userId, String nickname, @Schema(description = "친구가 된 시각") Instant friendsSince) {
}
