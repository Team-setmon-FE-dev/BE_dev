package com.example.setmon.friend.entity;

import com.example.setmon.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 거절·친구 삭제는 행을 지운다. 반대 방향 중복은 서비스에서 막는다. */
@Getter
@Entity
@Table(name = "friendships",
        uniqueConstraints = @UniqueConstraint(name = "uk_friendships_pair", columnNames = {"requester_id", "addressee_id"}),
        indexes = @Index(name = "idx_friendships_addressee", columnList = "addressee_id, status"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friendship extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "addressee_id", nullable = false)
    private Long addresseeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FriendshipStatus status;

    public static Friendship request(Long requesterId, Long addresseeId) {
        Friendship f = new Friendship();
        f.requesterId = requesterId;
        f.addresseeId = addresseeId;
        f.status = FriendshipStatus.PENDING;
        return f;
    }

    public void accept() {
        this.status = FriendshipStatus.ACCEPTED;
    }
}
