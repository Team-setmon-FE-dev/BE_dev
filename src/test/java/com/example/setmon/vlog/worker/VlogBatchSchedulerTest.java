package com.example.setmon.vlog.worker;

import com.example.setmon.vlog.entity.DailyVlog;
import com.example.setmon.vlog.service.VlogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** 00:10 배치 (3주차, B) */
@ExtendWith(MockitoExtension.class)
class VlogBatchSchedulerTest {

    /** KST 2026-11-18 00:10 = UTC 2026-11-17 15:10. UTC 날짜로 계산하면 하루가 어긋난다 */
    private static final Instant BATCH_TIME = Instant.parse("2026-11-17T15:10:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);
    private static final LocalDate YESTERDAY = LocalDate.of(2026, 11, 17);

    @Mock
    private VlogMaintenanceRepository repository;

    @Mock
    private VlogService vlogService;

    private VlogBatchScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new VlogBatchScheduler(repository, vlogService, Clock.fixed(BATCH_TIME, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("전날(KST) 클립이 있는 사용자마다 전날 브이로그를 요청한다")
    void requestsYesterdayForUsersWithClips() {
        given(repository.findUserIdsWithClipsOn(YESTERDAY)).willReturn(List.of(1L, 2L));
        given(repository.findAllByStaleTrueAndVlogDateBefore(TODAY)).willReturn(List.of());

        scheduler.enqueueDailyVlogs();

        verify(vlogService).requestGeneration(1L, YESTERDAY);
        verify(vlogService).requestGeneration(2L, YESTERDAY);
    }

    @Test
    @DisplayName("클립이 바뀐(stale) 지난 날짜의 브이로그도 다시 요청한다")
    void requestsStalePastVlogs() {
        LocalDate lastWeek = LocalDate.of(2026, 11, 10);
        given(repository.findUserIdsWithClipsOn(YESTERDAY)).willReturn(List.of());
        given(repository.findAllByStaleTrueAndVlogDateBefore(TODAY)).willReturn(List.of(DailyVlog.create(3L, lastWeek)));

        scheduler.enqueueDailyVlogs();

        verify(vlogService).requestGeneration(3L, lastWeek);
    }

    @Test
    @DisplayName("전날 클립도 있고 stale이기도 하면 한 번만 요청한다")
    void doesNotRequestTwice() {
        given(repository.findUserIdsWithClipsOn(YESTERDAY)).willReturn(List.of(1L));
        given(repository.findAllByStaleTrueAndVlogDateBefore(TODAY)).willReturn(List.of(DailyVlog.create(1L, YESTERDAY)));

        scheduler.enqueueDailyVlogs();

        verify(vlogService, times(1)).requestGeneration(1L, YESTERDAY);
    }

    @Test
    @DisplayName("한 사용자가 실패해도 나머지는 계속 요청한다")
    void continuesAfterOneFailure() {
        given(repository.findUserIdsWithClipsOn(YESTERDAY)).willReturn(List.of(1L, 2L));
        given(repository.findAllByStaleTrueAndVlogDateBefore(TODAY)).willReturn(List.of());
        willThrow(new RuntimeException("DB 오류")).given(vlogService).requestGeneration(1L, YESTERDAY);

        scheduler.enqueueDailyVlogs();

        verify(vlogService).requestGeneration(2L, YESTERDAY);
    }

    @Test
    @DisplayName("대상이 없으면 아무것도 요청하지 않는다")
    void nothingToDo() {
        given(repository.findUserIdsWithClipsOn(YESTERDAY)).willReturn(List.of());
        given(repository.findAllByStaleTrueAndVlogDateBefore(TODAY)).willReturn(List.of());

        scheduler.enqueueDailyVlogs();

        verify(vlogService, never()).requestGeneration(any(), any());
    }
}
