package com.example.setmon.vlog.worker;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FFmpeg 합성 모듈 (B 구현). 확정 규격:
 * <pre>
 * 클립: 720x1280(9:16), H.264/MP4, AAC, 1~2초, 약 1~2MB
 *
 * Input
 *   clipPaths  : S3에서 받은 로컬 클립 경로 (예: /tmp/clips/clip_1.mp4) — 이 순서대로 타임라인에 붙인다
 *   timeLabels : 각 클립 하단 촬영 시각 (예: "09:00", "14:30") — clipPaths와 같은 길이·같은 순서
 * Output
 *   VlogResult(vlogPath, thumbnailPath, status="SUCCESS")
 * </pre>
 * 워커는 시간순으로 정렬해서 넘기고, 업로드가 끝나면 결과 파일을 지운다.
 * 실패 시 예외(FFmpeg 에러 로그 포함)를 던지거나 status를 SUCCESS가 아닌 값으로 돌려주면 FAILED로 기록된다.
 */
public interface VlogComposer {

    VlogResult compose(List<String> clipPaths, List<String> timeLabels) throws Exception;
}
