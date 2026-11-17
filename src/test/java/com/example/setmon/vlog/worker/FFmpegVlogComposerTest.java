package com.example.setmon.vlog.worker;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FFmpegVlogComposerTest {

    /** ffmpeg가 PATH에 없는 환경(로컬 PC 등)에서는 실패 대신 건너뛴다. */
    @BeforeAll
    static void requireFfmpeg() {
        boolean available;
        try {
            Process p = new ProcessBuilder("ffmpeg", "-version").redirectErrorStream(true).start();
            p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
            available = p.waitFor() == 0;
        } catch (Exception e) {
            available = false;
        }
        assumeTrue(available, "ffmpeg not found in PATH");
    }

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

    // ===== 3주차: 폰마다 다른 클립이 섞여도 깨지지 않는지 =====

    private static final String SAMPLE = "src/test/resources/test_clip_1793659116805.mp4";

    /** 테스트용 클립을 ffmpeg로 만든다. audio=false면 소리 트랙이 아예 없는 파일 */
    private static String makeClip(java.nio.file.Path dir, String name, String size, int fps, boolean audio)
            throws Exception {
        String out = dir.resolve(name).toString();
        java.util.List<String> cmd = new java.util.ArrayList<>(List.of(
                "ffmpeg", "-y", "-nostdin",
                "-f", "lavfi", "-t", "1.5", "-i", "testsrc=size=" + size + ":rate=" + fps));
        if (audio) {
            cmd.addAll(List.of("-f", "lavfi", "-t", "1.5", "-i", "sine=frequency=440:sample_rate=8000"));
            cmd.addAll(List.of("-c:a", "aac", "-ac", "1"));
        }
        cmd.addAll(List.of("-c:v", "libx264", "-pix_fmt", "yuv420p", out));
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        assertEquals(0, p.waitFor(), "테스트 클립 생성 실패: " + name);
        return out;
    }

    /** ffprobe로 결과물의 스트림 정보를 읽는다 (예: "h264,720,1280" / "aac,44100,2") */
    private static String probe(String path, String stream, String entries) throws Exception {
        Process p = new ProcessBuilder("ffprobe", "-v", "error", "-select_streams", stream,
                "-show_entries", "stream=" + entries, "-of", "csv=p=0", path).start();
        String out = new String(p.getInputStream().readAllBytes()).trim();
        p.waitFor();
        return out;
    }

    private static void assertStandardOutput(VlogResult result) throws Exception {
        assertTrue(result.isSuccess());
        assertEquals("h264,720,1280", probe(result.vlogPath(), "v:0", "codec_name,width,height"));
        assertEquals("aac,44100,2", probe(result.vlogPath(), "a:0", "codec_name,sample_rate,channels"));
        assertTrue(new File(result.thumbnailPath()).length() > 0);
        new File(result.vlogPath()).delete();
        new File(result.thumbnailPath()).delete();
    }

    @Test
    void 소리_없는_클립이_섞여도_합성된다(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        String silent = makeClip(dir, "silent.mp4", "720x1280", 30, false);
        String withAudio = makeClip(dir, "audio.mp4", "720x1280", 30, true);

        VlogResult result = new FFmpegVlogComposer(dir.resolve("out").toString(), "ffmpeg")
                .compose(List.of(withAudio, silent, SAMPLE), List.of("09:00", "10:00", "11:00"));

        assertStandardOutput(result);
    }

    @Test
    void 모든_클립에_소리가_없어도_합성된다(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        String a = makeClip(dir, "a.mp4", "720x1280", 30, false);
        String b = makeClip(dir, "b.mp4", "720x1280", 30, false);

        VlogResult result = new FFmpegVlogComposer(dir.resolve("out").toString(), "ffmpeg")
                .compose(List.of(a, b), List.of("09:00", "10:00"));

        assertStandardOutput(result);
    }

    @Test
    void 해상도와_프레임레이트가_다른_클립이_섞여도_합성된다(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
            throws Exception {
        String landscape = makeClip(dir, "landscape.mp4", "1280x720", 24, true); // 가로 영상
        String small = makeClip(dir, "small.mp4", "480x640", 60, true);          // 4:3, 60fps
        String fullHd = makeClip(dir, "fullhd.mp4", "1080x1920", 30, false);     // 더 큰 해상도 + 무음

        VlogResult result = new FFmpegVlogComposer(dir.resolve("out").toString(), "ffmpeg")
                .compose(List.of(landscape, small, fullHd, SAMPLE), List.of("09:00", "10:00", "11:00", "12:00"));

        assertStandardOutput(result);
    }

    @Test
    void 제한_시간을_넘기면_FFmpeg를_끝내고_예외를_던진다(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) {
        java.nio.file.Path outDir = dir.resolve("out");
        VlogComposer composer = new FFmpegVlogComposer(outDir.toString(), "ffmpeg", java.time.Duration.ofMillis(1));

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> composer.compose(List.of(SAMPLE, SAMPLE, SAMPLE), List.of("09:00", "10:00", "11:00")));

        assertTrue(e.getMessage().contains("제한 시간 초과"), e.getMessage());
        // 실패한 결과물이 남지 않아야 한다
        String[] left = outDir.toFile().list();
        assertTrue(left == null || left.length == 0, "남은 파일: " + java.util.Arrays.toString(left));
    }
}