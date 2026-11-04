package com.example.setmon.auth.dto;

public record TokenResponse(Long userId, String accessToken, String refreshToken) {
}
