package com.example.setmon.vlog.worker;

import com.example.setmon.clip.entity.Clip;
import com.example.setmon.clip.service.ClipService;
import com.example.setmon.notification.NotificationType;
import com.example.setmon.notification.PushSender;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** 완성 알림 (3주차, B) */
@ExtendWith(MockitoExtension.class)
class VlogCompletionNotificationTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 17);

    @Mock private VlogJobService jobService;
    @Mock private ClipService clipService;
    @Mock private StorageService storageService;
    @Mock private VlogComposer composer;
    @Mock private PushSender pushSender;

    @InjectMocks private VlogWorker worker;

    private void givenOneClipComposes() throws Exception {
        given(clipService.getClips(1L, DATE)).willReturn(List.of(Clip.create(1L, DATE, 9,
                "users/1/clips/9.mp4", "users/1/clips/9.jpg", 1500, Instant.parse("2026-11-17T00:00:00Z"))));
        given(composer.compose(anyList(), anyList()))
                .willReturn(new VlogResult("/tmp/none/v.mp4", "/tmp/none/t.jpg", VlogResult.SUCCESS));
    }

    @Test
    @DisplayName("브이로그가 완성되면 그 날짜로 VLOG_DONE 알림을 보낸다")
    void sendsDoneNotification() throws Exception {
        givenOneClipComposes();

        worker.process(new VlogJob(10L, 1L, DATE));

        verify(pushSender).sendToUser(1L, NotificationType.VLOG_DONE, DATE);
    }

    @Test
    @DisplayName("합성에 실패하면 완성 알림을 보내지 않는다")
    void noNotificationOnFailure() throws Exception {
        given(clipService.getClips(1L, DATE)).willReturn(List.of(Clip.create(1L, DATE, 9,
                "users/1/clips/9.mp4", "users/1/clips/9.jpg", 1500, Instant.parse("2026-11-17T00:00:00Z"))));
        given(composer.compose(anyList(), anyList())).willThrow(new RuntimeException("FFmpeg exit code 1"));

        worker.process(new VlogJob(10L, 1L, DATE));

        verify(pushSender, never()).sendToUser(any(), any(), any());
    }

    @Test
    @DisplayName("알림 발송이 터져도 완성된 브이로그를 FAILED로 바꾸지 않는다")
    void pushErrorDoesNotFailVlog() throws Exception {
        givenOneClipComposes();
        willThrow(new RuntimeException("FCM down")).given(pushSender).sendToUser(any(), any(), any());

        worker.process(new VlogJob(10L, 1L, DATE));

        verify(jobService, never()).fail(any(), anyString());
    }
}
