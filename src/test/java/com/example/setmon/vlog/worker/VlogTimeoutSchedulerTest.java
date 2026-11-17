package com.example.setmon.vlog.worker;

import com.example.setmon.vlog.entity.VlogStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** 실패 처리 (3주차, B): 10분 타임아웃, 자동 1회 재시도 */
@ExtendWith(MockitoExtension.class)
class VlogTimeoutSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-11-17T03:00:00Z");

    @Mock
    private VlogJobService jobService;

    @Mock
    private VlogMaintenanceRepository repository;

    private VlogTimeoutScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new VlogTimeoutScheduler(jobService, repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("10분 넘게 PROCESSING인 작업을 TIMEOUT 사유로 실패 처리한다")
    void failsTimedOutJobs() {
        given(repository.findIdsByStatusStartedBefore(VlogStatus.PROCESSING, NOW.minusSeconds(600)))
                .willReturn(List.of(10L, 11L));

        scheduler.failTimedOut();

        verify(jobService).fail(eq(10L), startsWith("TIMEOUT"));
        verify(jobService).fail(eq(11L), startsWith("TIMEOUT"));
    }

    @Test
    @DisplayName("멈춘 작업이 없으면 아무것도 하지 않는다")
    void noTimedOutJobs() {
        given(repository.findIdsByStatusStartedBefore(any(), any())).willReturn(List.of());

        scheduler.failTimedOut();

        verify(jobService, never()).fail(anyLong(), anyString());
    }

    @Test
    @DisplayName("한 작업 처리에 실패해도 나머지는 계속한다")
    void continuesAfterTimeoutError() {
        given(repository.findIdsByStatusStartedBefore(any(), any())).willReturn(List.of(10L, 11L));
        willThrow(new IllegalStateException("없음")).given(jobService).fail(eq(10L), anyString());

        scheduler.failTimedOut();

        verify(jobService).fail(eq(11L), startsWith("TIMEOUT"));
    }

    @Test
    @DisplayName("처음 실패한 작업(retryCount=1)만 다시 대기열에 넣는다")
    void requeuesFirstFailuresOnly() {
        given(repository.findIdsByStatusAndRetryCount(VlogStatus.FAILED, 1)).willReturn(List.of(20L));
        given(repository.requeue(20L, VlogStatus.FAILED, VlogStatus.PENDING, 1)).willReturn(1);

        scheduler.retryFirstFailures();

        verify(repository).requeue(20L, VlogStatus.FAILED, VlogStatus.PENDING, 1);
    }

    @Test
    @DisplayName("재시도 대상이 없으면 아무것도 바꾸지 않는다")
    void nothingToRetry() {
        given(repository.findIdsByStatusAndRetryCount(VlogStatus.FAILED, 1)).willReturn(List.of());

        scheduler.retryFirstFailures();

        verify(repository, never()).requeue(anyLong(), any(), any(), anyInt());
    }
}
