package com.example.setmon.clip.repository;

import com.example.setmon.clip.entity.Clip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClipRepository extends JpaRepository<Clip, Long> {

    @Query("select count(distinct c.clipDate) from Clip c where c.userId = :userId")
    long countRecordDays(@Param("userId") Long userId);
}
