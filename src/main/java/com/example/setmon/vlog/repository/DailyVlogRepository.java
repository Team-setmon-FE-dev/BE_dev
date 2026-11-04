package com.example.setmon.vlog.repository;

import com.example.setmon.vlog.entity.DailyVlog;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyVlogRepository extends JpaRepository<DailyVlog, Long> {

    Optional<DailyVlog> findByUserIdAndVlogDate(Long userId, LocalDate vlogDate);
}
