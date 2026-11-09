package com.example.setmon.vlog.worker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VlogWorkerTest {

    @Mock
    private VlogJobService jobService;

    @Mock
    private VlogComposer composer;

    @InjectMocks
    private VlogWorker worker;

    @Test
    @DisplayName("PENDING 작업 처리 성공 시 DB 상태가 SUCCESS로 업데이트된다")
    void testWorkerProcessesPendingJobSuccessfully() throws Exception {
        // Given: PENDING 상태의 가상 작업 데이터 Mocking
        VlogJob mockJob = new VlogJob(1L, List.of("clip1.mp4", "clip2.mp4"), List.of("09:00", "14:00"));
        given(jobService.fetchPendingJob()).willReturn(Optional.of(mockJob));
        given(composer.compose(anyList(), anyList()))
                .willReturn(new VlogResult("/tmp/output/vlog_1.mp4", "/tmp/output/thumb_1.jpg", VlogResult.SUCCESS));

        // When: Worker 실행
        worker.processVlogJobs();

        // Then: DB 저장 메서드가 정상 인자와 함께 호출되었는지 검증
        verify(jobService).updateJobSuccess(eq(1L), eq("/tmp/output/vlog_1.mp4"), eq("/tmp/output/thumb_1.jpg"));
    }

    @Test
    @DisplayName("합성 도중 예외 발생 시 DB 상태가 FAILED로 업데이트된다")
    void testWorkerHandlesCompositionFailure() throws Exception {
        // Given: 합성 중 RuntimeException 발생 상황 Mocking
        VlogJob mockJob = new VlogJob(1L, List.of("clip1.mp4"), List.of("09:00"));
        given(jobService.fetchPendingJob()).willReturn(Optional.of(mockJob));
        given(composer.compose(anyList(), anyList()))
                .willThrow(new RuntimeException("FFmpeg process failed with exit code 1"));

        // When: Worker 실행
        worker.processVlogJobs();

        // Then: FAILED 상태 및 에러 메세지 업데이트 메서드 호출 검증
        verify(jobService).updateJobFailure(eq(1L), eq("FFmpeg process failed with exit code 1"));
    }
}