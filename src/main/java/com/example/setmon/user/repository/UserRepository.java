package com.example.setmon.user.repository;

import com.example.setmon.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    boolean existsByFriendCode(String friendCode);

    Optional<User> findByFriendCode(String friendCode);

    /** 매시 촬영 알림 대상: 촬영 알림 켜짐 + 알림 시간대(KST, 양끝 포함)에 hour가 들어가는 사용자 id */
    @Query("""
            select u.id from User u
            where u.captureNotifyOn = true
              and u.notifyStartHour <= :hour
              and u.notifyEndHour >= :hour
            """)
    List<Long> findCaptureNotifyTargetIds(@Param("hour") int hour);
}
