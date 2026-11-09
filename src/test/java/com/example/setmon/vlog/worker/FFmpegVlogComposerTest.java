package com.example.setmon.vlog.worker;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FFmpegVlogComposerTest {

    @Test
    void testComposeSuccess() throws Exception {
        // test/resources 내부 샘플 영상 파일 활용
        String sample1 = "src/test/resources/test_clip_1793659116805.mp4";
        String sample2 = "src/test/resources/test_clip_1793659123156.mp4";
        String sample3 = "src/test/resources/test_clip_1793659130200.mp4";

        List<String> clipPaths = List.of(sample1, sample2, sample3);
        List<String> timeLabels = List.of("09:00", "14:30", "19:00");

        VlogComposer composer = new FFmpegVlogComposer();
        VlogResult result = composer.compose(clipPaths, timeLabels);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Vlog composition should succeed");

        File vlogFile = new File(result.vlogPath());
        File thumbFile = new File(result.thumbnailPath());
        assertTrue(vlogFile.exists(), "Merged vlog mp4 file must exist");
        assertTrue(thumbFile.exists(), "Thumbnail jpg file must exist");
        assertTrue(vlogFile.length() > 0, "Vlog file size must be greater than 0");
    }
    @Test
    void testComposeWithEmptyClipsThrowsException() {
        VlogComposer composer = new FFmpegVlogComposer();
        assertThrows(IllegalArgumentException.class, () -> {
            composer.compose(List.of(), List.of());
        });
    }

    @Test
    void testComposeWithMismatchedSizeThrowsException() {
        VlogComposer composer = new FFmpegVlogComposer();
        List<String> clips = List.of("src/test/resources/test_clip_1793659116805.mp4");
        List<String> labels = List.of("09:00", "12:00"); // 개수 불일치

        assertThrows(IllegalArgumentException.class, () -> {
            composer.compose(clips, labels);
        });
    }

    @Test
    void testComposeWithInvalidFilePathThrowsException() {
        VlogComposer composer = new FFmpegVlogComposer();
        List<String> invalidClips = List.of("invalid/path/non_existent.mp4");
        List<String> labels = List.of("09:00");

        // FFmpeg 프로세스 실패 시 RuntimeException 발생 검증
        assertThrows(RuntimeException.class, () -> {
            composer.compose(invalidClips, labels);
        });
    }
}