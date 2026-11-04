package com.example.setmon.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Schema(example = "user@example.com") @NotBlank @Email @Size(max = 100) String email,
        @Schema(example = "password123") @NotBlank @Size(min = 8, max = 64, message = "비밀번호는 8자 이상이어야 합니다.") String password,
        @Schema(example = "스읍") @NotBlank @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다.") String nickname
) {
}
