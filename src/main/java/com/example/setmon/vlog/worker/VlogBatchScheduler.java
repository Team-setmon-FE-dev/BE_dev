package com.example.setmon.vlog.worker;

import com.example.setmon.common.config.TimeConfig;
import com.example.setmon.vlog.entity.DailyVlog;
import com.example.setmon.vlog.service.VlogService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 00:10(KST) 배치 (3주차, B). 아래 대상을 PENDING으로 만들면 VlogWorker가 하나씩 가져가 합성한다.
 * <ul>
 *   <li>전날 클립을 올린 사용자의 전날 브이로그</li>
 *   <li>클립이 삭제·교체되어 stale이 된, 오늘 이전 날짜의 브이로그 (다시 만들기)</li>
 * </ul>
 * 요청은 C의 VlogService.requestGeneration을 그대로 쓴다. 이미 최신(DONE·stale 아님)이거나
 * 대기·진행 중인 것은 거기서 건너뛴다. 오늘 날짜는 건드리지 않는다("지금 만들기" 또는 내일 배치).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vlog.worker.enabled", havingValue = "true", matchIfMissing = true)
public class VlogBatchScheduler {

    private final VlogMaintenanceRepository repository;
    private final VlogService vlogService;
    private final Clock clock;

    /** (userId, 날짜) 한 건 */
    private record Target(Long userId, LocalDate date) {
    }

    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    public void enqueueDailyVlogs() {
        try {
            LocalDate today = LocalDate.ofInstant(clock.instant(), TimeConfig.KST);
            LocalDate yesterday = today.minusDays(1);

            Set<Target> targets = new LinkedHashSet<>();
            for (Long userId : repository.findUserIdsWithClipsOn(yesterday)) {
                targets.add(new Target(userId, yesterday));
            }
            for (DailyVlog stale : repository.findAllByStaleTrueAndVlogDateBefore(today)) {
                targets.add(new Target(stale.getUserId(), stale.getVlogDate()));
            }

            int failed = 0;
            for (Target target : targets) {
                try {
                    vlogService.requestGeneration(target.userId(), target.date());
                } catch (Exception e) {
                    // 한 명이 실패해도 나머지는 계속한다
                    failed++;
                    log.error("브이로그 배치 요청 실패 userId={} date={}", target.userId(), target.date(), e);
                }
            }
            log.info("브이로그 배치 date={} targets={} failed={}", yesterday, targets.size(), failed);
        } catch (Exception e) {
            log.error("브이로그 배치 실패", e);
        }
    }
}
