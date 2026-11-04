package com.example.setmon.vlog.worker;

/**
 * VlogComposer 출력 (B와 합의한 규격).
 *
 * @param vlogPath      완성 브이로그 mp4 로컬 경로 (예: /tmp/output/daily_vlog_20261103.mp4)
 * @param thumbnailPath 썸네일 jpg 로컬 경로 (예: /tmp/output/thumb_20261103.jpg)
 * @param status        "SUCCESS"면 성공. 그 외 값은 실패로 처리한다.
 */
public record VlogResult(String vlogPath, String thumbnailPath, String status) {

    public static final String SUCCESS = "SUCCESS";

    public boolean isSuccess() {
        return SUCCESS.equals(status);
    }
}
