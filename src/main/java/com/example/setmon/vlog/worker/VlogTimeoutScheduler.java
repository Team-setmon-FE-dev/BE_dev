package com.example.setmon.vlog.worker;

import com.example.setmon.vlog.entity.VlogStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 실패 처리 (3주차, B). 1분마다 두 가지를 한다.
 * <ol>
 *   <li>10분 넘게 PROCESSING인 작업을 FAILED로 바꾼다 (C의 VlogJobService.fail 사용 → retryCount + 1)</li>
 *   <li>처음 실패한 작업(retryCount = 1)을 한 번 더 PENDING으로 넣는다. 두 번째 실패는 FAILED + 사유로 남아 앱에 표시된다</li>
 * </ol>
 * 사용자가 "지금 만들기"를 다시 누르거나 배치가 다시 요청하면 retryCount가 0으로 돌아가 재시도 기회도 다시 생긴다.
 * 워커(VlogWorker)와 다른 스레드에서 돌기 때문에 워커가 붙잡혀 있어도 동작한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vlog.worker.enabled", havingValue = "true", matchIfMissing = true)
public class VlogTimeoutScheduler {

    /** PROCESSING이 이 시간을 넘기면 멈춘 것으로 본다 */
    public static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(10);
    /** 이 횟수만큼 실패한 작업만 자동으로 다시 넣는다 (= 자동 재시도 1번) */
    public static final int RETRY_AFTER_FAILURES = 1;

    private final VlogJobService jobService;
    private final VlogMaintenanceRepository repository;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${vlog.worker.timeout-check-ms:60000}")
    public void check() {
        failTimedOut();
        retryFirstFailures();
    }

    void failTimedOut() {
        try {
            Instant threshold = clock.instant().minus(PROCESSING_TIMEOUT);
            List<Long> stuck = repository.findIdsByStatusStartedBefore(VlogStatus.PROCESSING, threshold);
            for (Long id : stuck) {
                try {
                    jobService.fail(id, "TIMEOUT: " + PROCESSING_TIMEOUT.toMinutes() + "분 안에 끝나지 않았습니다.");
                    log.warn("브이로그 타임아웃 → FAILED vlogId={}", id);
                } catch (Exception e) {
                    log.error("브이로그 타임아웃 처리 실패 vlogId={}", id, e);
                }
            }
        } catch (Exception e) {
            log.error("브이로그 타임아웃 검사 실패", e);
        }
    }

    void retryFirstFailures() {
        try {
            List<Long> failed = repository.findIdsByStatusAndRetryCount(VlogStatus.FAILED, RETRY_AFTER_FAILURES);
            for (Long id : failed) {
                try {
                    if (repository.requeue(id, VlogStatus.FAILED, VlogStatus.PENDING, RETRY_AFTER_FAILURES) > 0) {
                        log.warn("브이로그 자동 재시도 → PENDING vlogId={}", id);
                    }
                } catch (Exception e) {
                    log.error("브이로그 자동 재시도 실패 vlogId={}", id, e);
                }
            }
        } catch (Exception e) {
            log.error("브이로그 재시도 검사 실패", e);
        }
    }
}
