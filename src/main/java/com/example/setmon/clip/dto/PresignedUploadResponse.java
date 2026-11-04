package com.example.setmon.clip.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record PresignedUploadResponse(UploadTarget video, UploadTarget thumbnail,
                                      @Schema(description = "URL 만료 시각(10분)") Instant expiresAt) {

    public record UploadTarget(
            @Schema(example = "users/1/clips/2026-11-04/3f2a....mp4") String key,
            @Schema(description = "이 URL로 PUT 업로드") String uploadUrl,
            @Schema(example = "video/mp4") String contentType) {
    }
}
