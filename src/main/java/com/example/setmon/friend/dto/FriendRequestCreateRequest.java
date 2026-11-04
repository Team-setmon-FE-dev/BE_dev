package com.example.setmon.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record FriendRequestCreateRequest(@Schema(example = "K7Q2M9XA") @NotBlank String code) {
}
