package com.example.setmon.user.repository;

import com.example.setmon.user.entity.DeviceToken;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findAllByUserId(Long userId);

    /** 푸시 발송용: 여러 사용자의 토큰을 한 번에 (N+1 방지) */
    List<DeviceToken> findAllByUserIdIn(Collection<Long> userIds);

    /** FCM이 무효(UNREGISTERED·INVALID_ARGUMENT)라고 응답한 토큰 삭제. 삭제된 행 수 반환. */
    @Transactional
    @Modifying
    @Query("delete from DeviceToken d where d.token in :tokens")
    int deleteByTokenIn(@Param("tokens") Collection<String> tokens);

    @Modifying
    @Query("delete from DeviceToken d where d.userId = :userId")
    void deleteAllOfUser(@Param("userId") Long userId);
}
