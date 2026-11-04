package com.example.setmon.clip.repository;

import com.example.setmon.clip.entity.Clip;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClipRepository extends JpaRepository<Clip, Long> {

    @Query("select count(distinct c.clipDate) from Clip c where c.userId = :userId")
    long countRecordDays(@Param("userId") Long userId);

    Optional<Clip> findByUserIdAndClipDateAndSlotHour(Long userId, LocalDate clipDate, int slotHour);

    List<Clip> findAllByUserIdAndClipDateOrderBySlotHourAsc(Long userId, LocalDate clipDate);

    List<Clip> findAllByUserIdAndClipDateBetweenOrderByClipDateAscSlotHourAsc(Long userId, LocalDate from, LocalDate to);

    long countByUserIdAndClipDate(Long userId, LocalDate clipDate);

    @Modifying
    @Query("delete from Clip c where c.userId = :userId")
    void deleteAllOfUser(@Param("userId") Long userId);
}
