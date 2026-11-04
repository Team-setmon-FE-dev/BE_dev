package com.example.setmon.vlog.worker;

import com.example.setmon.clip.entity.Clip;
import com.example.setmon.clip.service.ClipService;
import com.example.setmon.common.config.TimeConfig;
import com.example.setmon.storage.StorageKeys;
import com.example.setmon.storage.StorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 몇 초마다 PENDING 하나를 가져와 처리한다.
 * PENDING → PROCESSING → (S3 다운로드 → VlogComposer(clipPaths, timeLabels) → S3 업로드) → DONE / FAILED
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vlog.worker.enabled", havingValue = "true", matchIfMissing = true)
public class VlogWorker {

    private final VlogJobService jobService;
    private final ClipService clipService;
    private final StorageService storageService;
    private final VlogComposer composer;

    private static final DateTimeFormatter TIME_LABEL = DateTimeFormatter.ofPattern("HH:mm");

    @Scheduled(fixedDelayString = "${vlog.worker.poll-delay-ms:5000}")
    public void poll() {
        Optional<VlogJob> job;
        try {
            job = jobService.claimNext();
        } catch (Exception e) {
            log.warn("브이로그 작업 가져오기 실패", e);
            return;
        }
        job.ifPresent(this::process);
    }

    void process(VlogJob job) {
        Path workDir = null;
        VlogResult result = null;
        try {
            List<Clip> clips = clipService.getClips(job.userId(), job.date());
            if (clips.isEmpty()) {
                jobService.clear(job.vlogId());
                return;
            }
            workDir = Files.createTempDirectory("vlog-" + job.vlogId() + "-");
            List<String> clipPaths = new ArrayList<>();
            List<String> timeLabels = new ArrayList<>();
            for (Clip clip : clips) { // slotHour 오름차순
                Path local = workDir.resolve("clip_%02d.mp4".formatted(clip.getSlotHour()));
                storageService.download(clip.getVideoKey(), local);
                clipPaths.add(local.toAbsolutePath().toString());
                timeLabels.add(LocalTime.ofInstant(clip.getRecordedAt(), TimeConfig.KST).format(TIME_LABEL));
            }

            result = composer.compose(clipPaths, timeLabels);
            if (result == null || !result.isSuccess()) {
                throw new IllegalStateException("VlogComposer status=" + (result == null ? "null" : result.status()));
            }

            String base = StorageKeys.newVlogBase(job.userId(), job.date());
            String videoKey = base + StorageKeys.VIDEO_EXT;
            String thumbKey = base + StorageKeys.THUMB_EXT;
            storageService.upload(videoKey, Path.of(result.vlogPath()), ClipService.VIDEO_CONTENT_TYPE);
            storageService.upload(thumbKey, Path.of(result.thumbnailPath()), ClipService.THUMB_CONTENT_TYPE);
            jobService.complete(job.vlogId(), videoKey, thumbKey);
            log.info("브이로그 완성 vlogId={} userId={} date={} clips={}", job.vlogId(), job.userId(), job.date(), clips.size());
            // TODO(3주차, B): 완성 알림 발송
        } catch (Exception e) {
            log.error("브이로그 생성 실패 vlogId={}", job.vlogId(), e);
            try {
                jobService.fail(job.vlogId(), e.getClass().getSimpleName() + ": " + e.getMessage());
            } catch (Exception inner) {
                log.error("FAILED 기록 실패 vlogId={}", job.vlogId(), inner);
            }
        } finally {
            deleteQuietly(workDir);
            if (result != null) {
                deleteFileQuietly(result.vlogPath());
                deleteFileQuietly(result.thumbnailPath());
            }
        }
    }

    private void deleteFileQuietly(String path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(path));
        } catch (Exception e) {
            log.warn("결과 파일 삭제 실패 {}", path, e);
        }
    }

    private void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            log.warn("임시 폴더 삭제 실패 {}", dir, e);
        }
    }
}
