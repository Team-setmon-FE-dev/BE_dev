package com.example.setmon.user.entity;

import com.example.setmon.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "device_tokens", indexes = @Index(name = "idx_device_tokens_user", columnList = "user_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    public static DeviceToken create(Long userId, String token) {
        DeviceToken deviceToken = new DeviceToken();
        deviceToken.userId = userId;
        deviceToken.token = token;
        return deviceToken;
    }

    /** 같은 기기에서 다른 계정으로 로그인하면 소유자를 바꾼다. */
    public void assignTo(Long userId) {
        this.userId = userId;
    }
}
