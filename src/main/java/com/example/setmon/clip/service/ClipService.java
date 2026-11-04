package com.example.setmon.clip.service;

import com.example.setmon.clip.dto.ClipResponse;
import com.example.setmon.clip.dto.CreateClipRequest;
import com.example.setmon.clip.dto.PresignedUploadResponse;
import com.example.setmon.clip.dto.PresignedUploadResponse.UploadTarget;
import com.example.setmon.clip.entity.Clip;
import com.example.setmon.clip.repository.ClipRepository;
import com.example.setmon.common.config.TimeConfig;
import com.example.setmon.common.exception.BusinessException;
import com.example.setmon.common.exception.ErrorCode;
import com.example.setmon.common.tx.AfterCommit;
import com.example.setmon.storage.StorageKeys;
import com.example.setmon.storage.StorageService;
import com.example.setmon.vlog.service.VlogService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClipService {

    public static final String VIDEO_CONTENT_TYPE = "video/mp4";
    public static final String THUMB_CONTENT_TYPE = "image/jpeg";
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(10);

    private final ClipRepository clipRepository;
    private final VlogService vlogService;
    private final StorageService storageService;
    private final Clock clock;

    // ===== 조회 (다른 도메인에서 사용) =====

    public long countRecordDays(Long userId) {
        return clipRepository.countRecordDays(userId);
    }

    public long countClips(Long userId, LocalDate date) {
        return clipRepository.countByUserIdAndClipDate(userId, date);
    }

    public List<Clip> getClips(Long userId, LocalDate date) {
        return clipRepository.findAllByUserIdAndClipDateOrderBySlotHourAsc(userId, date);
    }

    public List<Clip> getClipsBetween(Long userId, LocalDate from, LocalDate to) {
        return clipRepository.findAllByUserIdAndClipDateBetweenOrderByClipDateAscSlotHourAsc(userId, from, to);
    }

    // ===== 업로드 =====

    public PresignedUploadResponse issueUploadUrls(Long userId) {
        Instant now = clock.instant();
        String base = StorageKeys.newClipBase(userId, LocalDate.ofInstant(now, TimeConfig.KST));
        String videoKey = base + StorageKeys.VIDEO_EXT;
        String thumbKey = base + StorageKeys.THUMB_EXT;
        return new PresignedUploadResponse(
                new UploadTarget(videoKey, storageService.presignPut(videoKey, VIDEO_CONTENT_TYPE, UPLOAD_URL_TTL), VIDEO_CONTENT_TYPE),
                new UploadTarget(thumbKey, storageService.presignPut(thumbKey, THUMB_CONTENT_TYPE, UPLOAD_URL_TTL), THUMB_CONTENT_TYPE),
                now.plus(UPLOAD_URL_TTL));
    }

    @Transactional
    public ClipResponse register(Long userId, CreateClipRequest request) {
        validateDuration(request.durationMs());
        if (!StorageKeys.isOwnClipKey(userId, request.videoKey(), StorageKeys.VIDEO_EXT)
                || !StorageKeys.isOwnClipKey(userId, request.thumbKey(), StorageKeys.THUMB_EXT)) {
            throw new BusinessException(ErrorCode.INVALID_UPLOAD_KEY);
        }
        ClipSlotResolver.Slot slot = ClipSlotResolver.resolve(request.recordedAt(), clock.instant());
        if (!storageService.exists(request.videoKey()) || !storageService.exists(request.thumbKey())) {
            throw new BusinessException(ErrorCode.INVALID_UPLOAD_KEY, "S3에 파일이 없습니다. 업로드 후 등록해주세요.");
        }

        Optional<Clip> existing = clipRepository.findByUserIdAndClipDateAndSlotHour(userId, slot.date(), slot.hour());
        Clip clip;
        if (existing.isPresent()) {
            clip = existing.get();
            String oldVideo = clip.getVideoKey();
            String oldThumb = clip.getThumbKey();
            clip.replace(request.videoKey(), request.thumbKey(), request.durationMs(), request.recordedAt());
            deleteAfterCommit(oldVideo, request.videoKey());
            deleteAfterCommit(oldThumb, request.thumbKey());
        } else {
            clip = Clip.create(userId, slot.date(), slot.hour(),
                    request.videoKey(), request.thumbKey(), request.durationMs(), request.recordedAt());
            try {
                clipRepository.saveAndFlush(clip);
            } catch (DataIntegrityViolationException e) {
                throw new BusinessException(ErrorCode.CLIP_UPLOAD_CONFLICT);
            }
        }
        vlogService.markStale(userId, slot.date());
        return new ClipResponse(clip.getId(), clip.getClipDate(), clip.getSlotHour(), clip.getDurationMs(), existing.isPresent());
    }

    @Transactional
    public void delete(Long userId, Long clipId) {
        Clip clip = clipRepository.findById(clipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIP_NOT_FOUND));
        if (!clip.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        clipRepository.delete(clip);
        vlogService.markStale(userId, clip.getClipDate());
        deleteAfterCommit(clip.getVideoKey(), null);
        deleteAfterCommit(clip.getThumbKey(), null);
    }

    /** 탈퇴용. S3 파일은 탈퇴 처리에서 users/{id}/ 접두어로 한 번에 지운다. */
    @Transactional
    public void deleteAllOf(Long userId) {
        clipRepository.deleteAllOfUser(userId);
    }

    private void validateDuration(int durationMs) {
        if (durationMs < Clip.MIN_DURATION_MS) {
            throw new BusinessException(ErrorCode.CLIP_TOO_SHORT);
        }
        if (durationMs > Clip.MAX_DURATION_MS) {
            throw new BusinessException(ErrorCode.CLIP_TOO_LONG);
        }
    }

    private void deleteAfterCommit(String key, String keepKey) {
        if (key != null && !key.equals(keepKey)) {
            AfterCommit.run(() -> storageService.delete(key));
        }
    }
}
