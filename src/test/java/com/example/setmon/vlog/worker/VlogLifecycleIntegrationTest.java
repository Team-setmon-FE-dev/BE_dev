package com.example.setmon.vlog.worker;

import com.example.setmon.clip.entity.Clip;
import com.example.setmon.clip.repository.ClipRepository;
import com.example.setmon.vlog.entity.DailyVlog;
import com.example.setmon.vlog.entity.VlogStatus;
import com.example.setmon.vlog.repository.DailyVlogRepository;
import com.example.setmon.vlog.service.VlogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B의 쿼리(VlogMaintenanceRepository)와 C의 상태 전이(VlogJobService)를 실제 DB(H2)로 이어서 확인한다.
 * 테스트 설정은 vlog.worker.enabled=false 라 스케줄러는 직접 만들어 부른다.
 */
@SpringBootTest
class VlogLifecycleIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-11-17T03:00:00Z");
    private static final LocalDate DATE = LocalDate.of(2026, 11, 17);

    @Autowired
    private VlogJobService jobService;
    @Autowired
    private VlogService vlogService;
    @Autowired
    private VlogMaintenanceRepository maintenanceRepository;
    @Autowired
    private DailyVlogRepository dailyVlogRepository;
    @Autowired
    private ClipRepository clipRepository;

    private VlogTimeoutScheduler timeoutScheduler() {
        return new VlogTimeoutScheduler(jobService, maintenanceRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void cleanUp() {
        dailyVlogRepository.deleteAll();
        clipRepository.deleteAll();
    }

    private Long processingVlog(long userId, Instant startedAt) {
        DailyVlog vlog = DailyVlog.create(userId, DATE);
        vlog.requestGeneration();
        vlog.startProcessing(startedAt);
        return dailyVlogRepository.save(vlog).getId();
    }

    private DailyVlog reload(Long id) {
        return dailyVlogRepository.findById(id).orElseThrow();
    }

    @Test
    @DisplayName("멈춘 작업: 타임아웃 → 자동 재시도 1번 → 또 멈추면 FAILED + 사유로 남는다")
    void timeoutThenRetryOnceThenFail() {
        Long id = processingVlog(1L, NOW.minusSeconds(11 * 60));
        Long fresh = processingVlog(2L, NOW.minusSeconds(60)); // 1분 전 시작 → 건드리면 안 됨
        VlogTimeoutScheduler scheduler = timeoutScheduler();

        scheduler.check();
        DailyVlog afterFirst = reload(id);
        assertEquals(VlogStatus.PENDING, afterFirst.getStatus(), "첫 타임아웃은 자동 재시도");
        assertEquals(1, afterFirst.getRetryCount());
        assertEquals(VlogStatus.PROCESSING, reload(fresh).getStatus());

        // 워커가 재시도 작업을 가져갔는데 또 멈춤
        DailyVlog again = reload(id);
        again.startProcessing(NOW.minusSeconds(11 * 60));
        dailyVlogRepository.save(again);

        scheduler.check();
        DailyVlog afterSecond = reload(id);
        assertEquals(VlogStatus.FAILED, afterSecond.getStatus(), "두 번째는 FAILED로 남음");
        assertEquals(2, afterSecond.getRetryCount());
        assertTrue(afterSecond.getFailReason().startsWith("TIMEOUT"));
    }

    @Test
    @DisplayName("합성 실패(워커가 fail 호출) → 다음 검사에서 자동 재시도된다")
    void composeFailureIsRetried() {
        Long id = processingVlog(1L, NOW);
        jobService.fail(id, "RuntimeException: FFmpeg exit code 1");
        assertEquals(VlogStatus.FAILED, reload(id).getStatus());

        timeoutScheduler().check();

        assertEquals(VlogStatus.PENDING, reload(id).getStatus());
    }

    @Test
    @DisplayName("사용자가 다시 요청하면 재시도 기회도 다시 생긴다")
    void manualRequestResetsRetry() {
        Long id = processingVlog(1L, NOW);
        jobService.fail(id, "1");
        timeoutScheduler().check();                 // 1회 재시도
        DailyVlog v = reload(id);
        v.startProcessing(NOW);
        dailyVlogRepository.save(v);
        jobService.fail(id, "2");                   // 최종 FAILED
        timeoutScheduler().check();
        assertEquals(VlogStatus.FAILED, reload(id).getStatus());

        vlogService.requestGeneration(1L, DATE);    // 지금 만들기 / 다음 배치

        assertEquals(VlogStatus.PENDING, reload(id).getStatus());
        assertEquals(0, reload(id).getRetryCount());
    }

    @Test
    @DisplayName("배치 쿼리: 그날 클립 올린 사용자, 지난 날짜의 stale 브이로그")
    void batchQueries() {
        clipRepository.save(Clip.create(1L, DATE, 9, "users/1/clips/a.mp4", "users/1/clips/a.jpg", 1500, NOW));
        clipRepository.save(Clip.create(1L, DATE, 10, "users/1/clips/b.mp4", "users/1/clips/b.jpg", 1500, NOW));
        clipRepository.save(Clip.create(2L, DATE, 9, "users/2/clips/a.mp4", "users/2/clips/a.jpg", 1500, NOW));
        clipRepository.save(Clip.create(3L, DATE.minusDays(1), 9, "users/3/clips/a.mp4", "users/3/clips/a.jpg", 1500, NOW));

        DailyVlog stalePast = DailyVlog.create(4L, DATE.minusDays(3));
        stalePast.markStale();
        DailyVlog staleToday = DailyVlog.create(5L, DATE.plusDays(1));
        staleToday.markStale();
        dailyVlogRepository.saveAll(List.of(stalePast, staleToday, DailyVlog.create(6L, DATE.minusDays(2))));

        List<Long> users = maintenanceRepository.findUserIdsWithClipsOn(DATE);
        assertEquals(List.of(1L, 2L), users.stream().sorted().toList());

        List<DailyVlog> stale = maintenanceRepository.findAllByStaleTrueAndVlogDateBefore(DATE.plusDays(1));
        assertEquals(1, stale.size());
        assertEquals(4L, stale.get(0).getUserId());
    }
}
