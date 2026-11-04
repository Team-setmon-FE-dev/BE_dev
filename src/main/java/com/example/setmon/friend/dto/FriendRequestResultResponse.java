package com.example.setmon.friend.dto;

import com.example.setmon.friend.entity.FriendshipStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record FriendRequestResultResponse(
        Long requestId,
        @Schema(description = "PENDING: 요청 보냄 / ACCEPTED: 상대가 이미 나에게 요청해 둬서 바로 친구가 됨") FriendshipStatus status
) {
}
