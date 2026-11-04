package com.example.setmon.user.repository;

import com.example.setmon.user.entity.DeviceToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findAllByUserId(Long userId);

    @Modifying
    @Query("delete from DeviceToken d where d.userId = :userId")
    void deleteAllOfUser(@Param("userId") Long userId);
}
