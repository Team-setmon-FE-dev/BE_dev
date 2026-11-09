package com.example.setmon.vlog.worker;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class FFmpegVlogComposer implements VlogComposer {

    private final String outputDirPath;
    private final String ffmpegPath;

    public FFmpegVlogComposer() {
        this.outputDirPath = "/tmp/output";
        this.ffmpegPath = "ffmpeg";
    }

    public FFmpegVlogComposer(String outputDirPath, String ffmpegPath) {
        this.outputDirPath = outputDirPath;
        this.ffmpegPath = ffmpegPath;
    }

    @Override
    public VlogResult compose(List<String> clipPaths, List<String> timeLabels) throws Exception {
        if (clipPaths == null || clipPaths.isEmpty()) {
            throw new IllegalArgumentException("clipPaths must not be empty");
        }
        if (clipPaths.size() != timeLabels.size()) {
            throw new IllegalArgumentException("clipPaths and timeLabels size mismatch");
        }

        File outputDir = new File(outputDirPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String outputId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        File vlogFile = new File(outputDir, "daily_vlog_" + outputId + ".mp4");
        File thumbFile = new File(outputDir, "thumb_" + outputId + ".jpg");

        // 1. FFmpeg 인코딩 (720x1280 스케일링 + drawtext 자막 + concat)
        List<String> command = buildFFmpegCommand(clipPaths, timeLabels, vlogFile.getAbsolutePath());
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        StringBuilder logOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logOutput.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg exit code " + exitCode + ": " + logOutput.toString());
        }

        // 2. 대표 썸네일 추출 (첫 프레임)
        List<String> thumbCommand = List.of(
                ffmpegPath, "-y",
                "-i", vlogFile.getAbsolutePath(),
                "-ss", "00:00:00.000",
                "-vframes", "1",
                thumbFile.getAbsolutePath()
        );
        Process thumbProcess = new ProcessBuilder(thumbCommand).start();
        int thumbExitCode = thumbProcess.waitFor();

        if (thumbExitCode != 0 || !thumbFile.exists()) {
            throw new RuntimeException("Thumbnail generation failed");
        }

        return new VlogResult(vlogFile.getAbsolutePath(), thumbFile.getAbsolutePath(), VlogResult.SUCCESS);
    }

    private List<String> buildFFmpegCommand(List<String> clipPaths, List<String> timeLabels, String outputPath) {
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-y");

        for (String path : clipPaths) {
            command.add("-i");
            command.add(path);
        }

        StringBuilder filterComplex = new StringBuilder();
        int n = clipPaths.size();

        // drawtext 필터 내 콜론(:) 이스케이프 (09:00 -> 09\:00)
        for (int i = 0; i < n; i++) {
            String label = timeLabels.get(i).replace(":", "\\:");
            filterComplex.append(String.format(
                    "[%d:v]scale=720:1280:force_original_aspect_ratio=decrease,pad=720:1280:(ow-iw)/2:(oh-ih)/2,setsar=1," +
                            "drawtext=text='%s':fontcolor=white:fontsize=36:box=1:boxcolor=black@0.5:boxborderw=6:x=30:y=h-70[v%d];",
                    i, label, i
            ));
        }

        for (int i = 0; i < n; i++) {
            filterComplex.append(String.format("[v%d][%d:a]", i, i));
        }
        filterComplex.append(String.format("concat=n=%d:v=1:a=1[outv][outa]", n));

        command.add("-filter_complex");
        command.add(filterComplex.toString());
        command.add("-map");
        command.add("[outv]");
        command.add("-map");
        command.add("[outa]");
        command.add("-c:v");
        command.add("libx264");
        command.add("-c:a");
        command.add("aac");
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add(outputPath);

        return command;
    }
}