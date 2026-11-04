package com.example.setmon.common.tx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** DB 커밋이 끝난 뒤에 실행 (S3 파일 삭제처럼 되돌릴 수 없는 작업용). 실패는 로그만 남긴다. */
@Slf4j
public final class AfterCommit {

    private AfterCommit() {
    }

    public static void run(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeRun(task);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safeRun(task);
            }
        });
    }

    private static void safeRun(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.warn("커밋 후 작업 실패", e);
        }
    }
}
