package com.example.setmon.clip.controller;

import com.example.setmon.clip.dto.ClipResponse;
import com.example.setmon.clip.dto.CreateClipRequest;
import com.example.setmon.clip.dto.PresignedUploadResponse;
import com.example.setmon.common.exception.BusinessException;
import com.example.setmon.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "클립")
@RestController
@RequestMapping("/api")
public class ClipController {

    @Operation(summary = "[미구현] 업로드 URL 발급 (F-11)",
            description = "영상(mp4)·썸네일(jpeg) presigned PUT URL과 S3 키. 10분 유효")
    @PostMapping("/uploads/presigned-url")
    public PresignedUploadResponse issuePresignedUrl(@Parameter(hidden = true) @AuthenticationPrincipal Long userId) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Operation(summary = "[미구현] 클립 등록 (F-12)", description = """
            서버가 recordedAt(KST)으로 날짜·슬롯을 정한다. 같은 슬롯이면 교체(replaced=true).
            정각 후 2분까지는 직전 슬롯으로 인정.
            에러: PAST_SLOT, CLIP_TOO_SHORT, CLIP_TOO_LONG, INVALID_UPLOAD_KEY""")
    @PostMapping("/clips")
    @ResponseStatus(HttpStatus.CREATED)
    public ClipResponse createClip(@Parameter(hidden = true) @AuthenticationPrincipal Long userId,
                                   @Valid @RequestBody CreateClipRequest request) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Operation(summary = "[미구현] 내 클립 삭제 (F-14)", description = "그날 브이로그 stale 처리. 204. 에러: CLIP_NOT_FOUND, FORBIDDEN")
    @DeleteMapping("/clips/{clipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClip(@Parameter(hidden = true) @AuthenticationPrincipal Long userId, @PathVariable Long clipId) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }
}
