package com.example.setmon.vlog.worker;

import com.example.setmon.common.tx.AfterCommit;
import com.example.setmon.storage.StorageService;
import com.example.setmon.vlog.entity.DailyVlog;
import com.example.setmon.vlog.repository.DailyVlogRepository;
import java.time.Clock;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 워커의 DB 상태 전이. 각 메서드가 짧은 트랜잭션 하나(FFmpeg 도는 동안 DB 잠금을 잡지 않도록). */
@Service
@RequiredArgsConstructor
public class VlogJobService {

    private static final int MAX_REASON_LENGTH = 1000;

    private final DailyVlogRepository dailyVlogRepository;
    private final StorageService storageService;
    private final Clock clock;

    /** PENDING 하나를 PROCESSING으로 바꾸고 가져온다. */
    @Transactional
    public Optional<VlogJob> claimNext() {
        return dailyVlogRepository.lockNextPendingId()
                .flatMap(dailyVlogRepository::findById)
                .map(vlog -> {
                    vlog.startProcessing(clock.instant());
                    return new VlogJob(vlog.getId(), vlog.getUserId(), vlog.getVlogDate());
                });
    }

    @Transactional
    public void complete(Long vlogId, String videoKey, String thumbKey) {
        DailyVlog vlog = get(vlogId);
        deleteOldOutputs(vlog);
        vlog.complete(videoKey, thumbKey);
    }

    @Transactional
    public void fail(Long vlogId, String reason) {
        String trimmed = reason == null ? "알 수 없는 오류"
                : reason.length() > MAX_REASON_LENGTH ? reason.substring(0, MAX_REASON_LENGTH) : reason;
        get(vlogId).fail(trimmed);
    }

    /** 그날 클립이 모두 지워진 경우 */
    @Transactional
    public void clear(Long vlogId) {
        DailyVlog vlog = get(vlogId);
        deleteOldOutputs(vlog);
        vlog.clear();
    }

    private void deleteOldOutputs(DailyVlog vlog) {
        String oldVideo = vlog.getVideoKey();
        String oldThumb = vlog.getThumbKey();
        if (oldVideo != null) {
            AfterCommit.run(() -> storageService.delete(oldVideo));
        }
        if (oldThumb != null) {
            AfterCommit.run(() -> storageService.delete(oldThumb));
        }
    }

    private DailyVlog get(Long vlogId) {
        return dailyVlogRepository.findById(vlogId)
                .orElseThrow(() -> new IllegalStateException("daily_vlog 없음: " + vlogId));
    }
}
