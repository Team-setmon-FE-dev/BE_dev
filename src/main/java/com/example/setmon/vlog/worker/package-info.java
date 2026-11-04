/**
 * 하루 브이로그 워커.
 * - C: VlogWorker(폴링·다운로드·업로드·상태 전이), VlogJobService(트랜잭션)
 * - B: VlogComposer 구현(FFmpeg), 00:10 배치, 10분 타임아웃·재시도
 */
package com.example.setmon.vlog.worker;
