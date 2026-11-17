package com.example.setmon.vlog.worker;

import com.example.setmon.vlog.entity.DailyVlog;
import com.example.setmon.vlog.entity.VlogStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * 브이로그 생성 주기(B: 00:10 배치, 10분 타임아웃, 자동 재시도) 전용 조회·갱신.
 * C의 DailyVlogRepository·ClipRepository를 고치지 않으려고 따로 둔다.
 */
public interface VlogMaintenanceRepository extends JpaRepository<DailyVlog, Long> {

    /** 이 시각 전에 시작했는데 아직 PROCESSING인 작업 (멈춘 작업) */
    @Query("select v.id from DailyVlog v where v.status = :status and v.startedAt < :threshold")
    List<Long> findIdsByStatusStartedBefore(@Param("status") VlogStatus status,
                                            @Param("threshold") Instant threshold);

    /** 자동 재시도 대상: 처음 실패한 작업 (retryCount = 1) */
    @Query("select v.id from DailyVlog v where v.status = :status and v.retryCount = :retryCount")
    List<Long> findIdsByStatusAndRetryCount(@Param("status") VlogStatus status,
                                            @Param("retryCount") int retryCount);

    /**
     * 처음 실패한 작업을 다시 대기열에 넣는다. retryCount는 그대로 둔다
     * (DailyVlog.requestGeneration()은 retryCount를 0으로 되돌려 재시도가 끝없이 반복되므로 쓰지 않는다).
     * 조건을 WHERE에 넣어, 그 사이 상태가 바뀐 작업은 건드리지 않는다.
     *
     * @return 바뀐 행 수 (0 또는 1)
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update DailyVlog v set v.status = :pending "
            + "where v.id = :id and v.status = :failed and v.retryCount = :retryCount")
    int requeue(@Param("id") Long id,
                @Param("failed") VlogStatus failed,
                @Param("pending") VlogStatus pending,
                @Param("retryCount") int retryCount);

    /** 클립이 바뀌어 다시 만들어야 하는, 이 날짜 이전의 브이로그 */
    List<DailyVlog> findAllByStaleTrueAndVlogDateBefore(LocalDate date);

    /** 그 날짜에 클립을 하나라도 올린 사용자 */
    @Query("select distinct c.userId from Clip c where c.clipDate = :clipDate")
    List<Long> findUserIdsWithClipsOn(@Param("clipDate") LocalDate clipDate);
}
