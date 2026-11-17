package com.example.setmon.vlog.worker;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * FFmpeg로 클립을 시간순으로 이어붙여 하루 브이로그를 만든다.
 *
 * <p>폰마다 다른 클립이 섞여도 깨지지 않도록 클립마다 형식을 맞춘 뒤 붙인다.
 * <ul>
 *   <li>영상: 30fps, 720x1280(비율 유지 + 검은 여백), yuv420p. 회전 정보는 FFmpeg가 알아서 반영한다</li>
 *   <li>소리: 44.1kHz 스테레오. 소리가 없는 클립은 그 길이만큼 무음을 넣는다</li>
 * </ul>
 * <p>클립을 한 명령에 모두 넣어 붙이면 FFmpeg가 전부를 동시에 풀어 메모리에 쌓는다
 * (24클립이면 400MB를 넘겨 서버에서 강제 종료됨). 그래서 두 단계로 나눈다.
 * <ol>
 *   <li>클립을 하나씩 같은 형식의 조각(.ts)으로 변환 — 메모리가 클립 수와 무관</li>
 *   <li>조각들을 재인코딩 없이 이어붙임</li>
 * </ol>
 * FFmpeg가 멈추면 워커 스레드가 통째로 막히므로 제한 시간을 넘기면 강제 종료하고 예외를 던진다.
 */
@Component
public class FFmpegVlogComposer implements VlogComposer {

    /** 워커의 10분 타임아웃보다 먼저 끝나야 FAILED 처리와 겹치지 않는다 */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(8);
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(20);
    private static final double FALLBACK_CLIP_SECONDS = 2.0;
    private static final int MAX_ERROR_LOG_CHARS = 2000;

    /** 자막 글꼴. 서버 이미지(ubuntu)에 들어 있는 것부터 찾고, 없으면 FFmpeg 기본 글꼴에 맡긴다 */
    private static final List<String> FONT_CANDIDATES = List.of(
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
            "/System/Library/Fonts/Helvetica.ttc");

    private final String outputDirPath;
    private final String ffmpegPath;
    private final String ffprobePath;
    private final Duration timeout;

    public FFmpegVlogComposer() {
        this("/tmp/output", "ffmpeg");
    }

    public FFmpegVlogComposer(String outputDirPath, String ffmpegPath) {
        this(outputDirPath, ffmpegPath, DEFAULT_TIMEOUT);
    }

    public FFmpegVlogComposer(String outputDirPath, String ffmpegPath, Duration timeout) {
        this.outputDirPath = outputDirPath;
        this.ffmpegPath = ffmpegPath;
        this.ffprobePath = ffmpegPath.endsWith("ffmpeg")
                ? ffmpegPath.substring(0, ffmpegPath.length() - "ffmpeg".length()) + "ffprobe"
                : "ffprobe";
        this.timeout = timeout;
    }

    @Override
    public VlogResult compose(List<String> clipPaths, List<String> timeLabels) throws Exception {
        if (clipPaths == null || clipPaths.isEmpty()) {
            throw new IllegalArgumentException("clipPaths must not be empty");
        }
        if (timeLabels == null || clipPaths.size() != timeLabels.size()) {
            throw new IllegalArgumentException("clipPaths and timeLabels size mismatch");
        }

        File outputDir = new File(outputDirPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        List<ClipInfo> clips = new ArrayList<>();
        for (String path : clipPaths) {
            clips.add(probe(path));
        }

        String outputId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        File vlogFile = new File(outputDir, "daily_vlog_" + outputId + ".mp4");
        File thumbFile = new File(outputDir, "thumb_" + outputId + ".jpg");

        long deadline = System.nanoTime() + timeout.toNanos();
        Path workDir = Files.createTempDirectory(outputDir.toPath(), "segments-" + outputId + "-");
        try {
            // 1단계: 클립마다 표준 형식 조각으로 변환
            List<Path> segments = new ArrayList<>();
            for (int i = 0; i < clips.size(); i++) {
                Path segment = workDir.resolve(String.format(Locale.ROOT, "seg_%03d.ts", i));
                run(buildSegmentCommand(clips.get(i), timeLabels.get(i), segment.toString()),
                        remaining(deadline), "FFmpeg");
                segments.add(segment);
            }

            // 2단계: 재인코딩 없이 이어붙이기
            Path list = workDir.resolve("list.txt");
            StringBuilder listText = new StringBuilder();
            for (Path segment : segments) {
                listText.append("file '").append(segment.toAbsolutePath()).append("'\n");
            }
            Files.writeString(list, listText.toString(), StandardCharsets.UTF_8);
            run(List.of(ffmpegPath, "-y", "-nostdin",
                    "-f", "concat", "-safe", "0", "-i", list.toString(),
                    "-c", "copy",
                    "-bsf:a", "aac_adtstoasc",
                    "-movflags", "+faststart", // 앱에서 받는 즉시 재생되게
                    vlogFile.getAbsolutePath()), remaining(deadline), "FFmpeg");

            // 대표 썸네일 (첫 프레임)
            run(List.of(ffmpegPath, "-y", "-nostdin",
                    "-i", vlogFile.getAbsolutePath(),
                    "-frames:v", "1", "-q:v", "3",
                    thumbFile.getAbsolutePath()), PROBE_TIMEOUT, "Thumbnail");
            if (!vlogFile.exists() || vlogFile.length() == 0 || !thumbFile.exists()) {
                throw new RuntimeException("FFmpeg가 결과 파일을 만들지 못했습니다.");
            }
        } catch (Exception e) {
            // 실패한 결과물이 /tmp에 쌓이지 않게 지운다
            vlogFile.delete();
            thumbFile.delete();
            throw e;
        } finally {
            deleteRecursively(workDir);
        }

        return new VlogResult(vlogFile.getAbsolutePath(), thumbFile.getAbsolutePath(), VlogResult.SUCCESS);
    }

    /** 클립 하나의 소리 유무와 길이 */
    private record ClipInfo(String path, boolean hasAudio, double durationSeconds) {
    }

    private ClipInfo probe(String path) throws Exception {
        String out;
        try {
            out = run(List.of(ffprobePath, "-v", "error",
                    "-show_entries", "stream=codec_type:format=duration",
                    "-of", "default=noprint_wrappers=1",
                    path), PROBE_TIMEOUT, "ffprobe");
        } catch (RuntimeException e) {
            throw new RuntimeException("클립을 읽을 수 없습니다: " + path + " (" + e.getMessage() + ")", e);
        }
        boolean hasVideo = false;
        boolean hasAudio = false;
        double duration = FALLBACK_CLIP_SECONDS;
        for (String line : out.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.equals("codec_type=video")) {
                hasVideo = true;
            } else if (trimmed.equals("codec_type=audio")) {
                hasAudio = true;
            } else if (trimmed.startsWith("duration=")) {
                try {
                    double parsed = Double.parseDouble(trimmed.substring("duration=".length()));
                    if (parsed > 0) {
                        duration = parsed;
                    }
                } catch (NumberFormatException ignored) {
                    // "N/A" 등: 기본 길이를 쓴다
                }
            }
        }
        if (!hasVideo) {
            throw new RuntimeException("영상 트랙이 없는 클립입니다: " + path);
        }
        return new ClipInfo(path, hasAudio, duration);
    }

    /** 남은 시간. 전체 합성이 제한 시간 하나를 나눠 쓴다 */
    private static Duration remaining(long deadlineNanos) {
        return Duration.ofNanos(Math.max(1, deadlineNanos - System.nanoTime()));
    }

    /** 클립 하나를 표준 형식 조각으로 변환하는 명령 */
    private List<String> buildSegmentCommand(ClipInfo clip, String timeLabel, String outputPath) {
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-y");
        command.add("-nostdin");
        command.add("-i");
        command.add(clip.path());
        if (!clip.hasAudio()) {
            // 소리 없는 클립: 무음을 넣는다 (길이는 -shortest 로 영상에 맞춰진다)
            command.addAll(List.of("-f", "lavfi", "-i", "anullsrc=channel_layout=stereo:sample_rate=44100"));
        }

        String font = findFont();
        String videoFilter = "setpts=PTS-STARTPTS,fps=30,"
                + "scale=720:1280:force_original_aspect_ratio=decrease,"
                + "pad=720:1280:(ow-iw)/2:(oh-ih)/2,setsar=1,format=yuv420p,"
                + "drawtext=" + (font == null ? "" : "fontfile='" + font + "':")
                + "text='" + escapeLabel(timeLabel) + "':fontcolor=white:fontsize=36:"
                + "box=1:boxcolor=black@0.5:boxborderw=6:x=30:y=h-70";
        // apad + -shortest: 소리가 영상보다 짧으면 무음으로 채우고, 길면 영상 길이에서 자른다.
        // 조각마다 영상·소리 길이가 같아야 이어붙였을 때 소리가 밀리지 않는다
        String audioFilter = "asetpts=PTS-STARTPTS,aresample=44100,"
                + "aformat=sample_fmts=fltp:channel_layouts=stereo,apad";

        command.addAll(List.of(
                "-map", "0:v:0",
                "-map", clip.hasAudio() ? "0:a:0" : "1:a:0",
                "-vf", videoFilter,
                "-af", audioFilter,
                "-shortest",
                "-c:v", "libx264",
                "-preset", "veryfast", // 서버(vCPU 2개)에서 24클립을 2분 안에 끝내기 위해
                "-crf", "23",
                "-pix_fmt", "yuv420p",
                "-r", "30",
                "-c:a", "aac",
                "-b:a", "128k",
                "-ar", "44100",
                "-ac", "2",
                "-f", "mpegts",
                outputPath));
        return command;
    }

    /** drawtext 안에서 뜻이 있는 문자를 막는다. 라벨은 "14:30" 같은 시각이라 숫자·콜론만 남긴다 */
    private static String escapeLabel(String label) {
        String safe = label == null ? "" : label.replaceAll("[^0-9A-Za-z: ]", "");
        return safe.replace(":", "\\:");
    }

    private static void deleteRecursively(Path dir) {
        try (java.util.stream.Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // 임시 폴더 삭제 실패는 합성 결과에 영향이 없다
        }
    }

    private static String findFont() {
        for (String candidate : FONT_CANDIDATES) {
            if (new File(candidate).isFile()) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 명령을 실행하고 출력(표준출력+에러)을 돌려준다.
     * 출력은 파일로 받는다. 파이프로 받으면 프로세스가 멈췄을 때 읽는 쪽도 같이 멈춰 제한 시간이 듣지 않는다.
     *
     * @throws RuntimeException 종료 코드가 0이 아니거나 제한 시간을 넘긴 경우 (FFmpeg 로그 끝부분 포함)
     */
    private String run(List<String> command, Duration limit, String name) throws IOException, InterruptedException {
        Path log = Files.createTempFile("ffmpeg-", ".log");
        Process process = null;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(log.toFile())
                    .start();
            boolean finished = process.waitFor(limit.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
                throw new RuntimeException(name + " 제한 시간 초과 (" + limit.toSeconds() + "초): " + tail(log));
            }
            String output = readLeniently(log);
            if (process.exitValue() != 0) {
                throw new RuntimeException(name + " exit code " + process.exitValue() + ": " + tail(output));
            }
            return output;
        } catch (InterruptedException e) {
            if (process != null) {
                process.destroyForcibly();
            }
            throw e;
        } finally {
            Files.deleteIfExists(log);
        }
    }

    /** FFmpeg 로그에는 깨진 글자가 섞일 수 있어 엄격한 디코딩(readString)을 쓰지 않는다 */
    private static String readLeniently(Path log) throws IOException {
        return new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
    }

    private static String tail(Path log) {
        try {
            return tail(readLeniently(log));
        } catch (IOException e) {
            return "(로그를 읽지 못함)";
        }
    }

    private static String tail(String text) {
        return text.length() > MAX_ERROR_LOG_CHARS ? text.substring(text.length() - MAX_ERROR_LOG_CHARS) : text;
    }
}
