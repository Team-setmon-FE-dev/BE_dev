package com.example.setmon.vlog.repository;

import com.example.setmon.vlog.entity.DailyVlog;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyVlogRepository extends JpaRepository<DailyVlog, Long> {

    Optional<DailyVlog> findByUserIdAndVlogDate(Long userId, LocalDate vlogDate);

    List<DailyVlog> findAllByUserIdAndVlogDateBetween(Long userId, LocalDate from, LocalDate to);

    /** 워커용: PENDING 하나를 잠그고 가져온다. 다른 워커가 잡은 행은 건너뜀(MySQL 8). */
    @Query(value = "SELECT id FROM daily_vlogs WHERE status = 'PENDING' ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    Optional<Long> lockNextPendingId();

    @Modifying
    @Query("delete from DailyVlog v where v.userId = :userId")
    void deleteAllOfUser(@Param("userId") Long userId);
}
