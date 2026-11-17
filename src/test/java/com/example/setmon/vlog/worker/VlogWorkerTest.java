package com.example.setmon.vlog.worker;

import com.example.setmon.clip.entity.Clip;
import com.example.setmon.clip.service.ClipService;
import com.example.setmon.storage.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VlogWorkerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 17);

    @Mock
    private VlogJobService jobService;

    @Mock
    private ClipService clipService;

    @Mock
    private StorageService storageService;

    @Mock
    private VlogComposer composer;

    @InjectMocks
    private VlogWorker worker;

    private static Clip clip(int hour) {
        return Clip.create(1L, DATE, hour, "users/1/clips/" + hour + ".mp4", "users/1/clips/" + hour + ".jpg",
                1500, Instant.parse("2026-11-17T00:00:00Z").plusSeconds(3600L * (hour - 9)));
    }

    @Test
    @DisplayName("합성 성공 시 S3 업로드 후 DONE으로 완료 처리한다")
    void completesOnSuccess() throws Exception {
        VlogJob job = new VlogJob(10L, 1L, DATE);
        given(clipService.getClips(1L, DATE)).willReturn(List.of(clip(9), clip(14)));
        given(composer.compose(anyList(), anyList()))
                .willReturn(new VlogResult("/tmp/none/vlog_10.mp4", "/tmp/none/thumb_10.jpg", VlogResult.SUCCESS));

        worker.process(job);

        verify(jobService).complete(eq(10L), startsWith("users/1/vlogs/2026-11-17/"), startsWith("users/1/vlogs/2026-11-17/"));
        verify(jobService, never()).fail(any(), anyString());
    }

    @Test
    @DisplayName("합성 도중 예외가 나면 FAILED로 기록한다")
    void failsOnComposeError() throws Exception {
        VlogJob job = new VlogJob(10L, 1L, DATE);
        given(clipService.getClips(1L, DATE)).willReturn(List.of(clip(9)));
        given(composer.compose(anyList(), anyList()))
                .willThrow(new RuntimeException("FFmpeg process failed with exit code 1"));

        worker.process(job);

        verify(jobService).fail(10L, "RuntimeException: FFmpeg process failed with exit code 1");
        verify(jobService, never()).complete(any(), any(), any());
    }

    @Test
    @DisplayName("클립이 없으면 작업을 지운다")
    void clearsWhenNoClips() throws Exception {
        VlogJob job = new VlogJob(10L, 1L, DATE);
        given(clipService.getClips(1L, DATE)).willReturn(List.of());

        worker.process(job);

        verify(jobService).clear(10L);
        verify(composer, never()).compose(anyList(), anyList());
    }
}
