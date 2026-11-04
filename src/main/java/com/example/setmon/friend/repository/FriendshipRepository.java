package com.example.setmon.friend.repository;

import com.example.setmon.friend.entity.Friendship;
import com.example.setmon.friend.entity.FriendshipStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /** 방향과 무관하게 두 사람 사이의 관계 */
    @Query("""
            select f from Friendship f
            where (f.requesterId = :a and f.addresseeId = :b)
               or (f.requesterId = :b and f.addresseeId = :a)
            """)
    Optional<Friendship> findBetween(@Param("a") Long a, @Param("b") Long b);

    @Query("""
            select count(f) > 0 from Friendship f
            where f.status = :status
              and ((f.requesterId = :a and f.addresseeId = :b)
                or (f.requesterId = :b and f.addresseeId = :a))
            """)
    boolean existsBetweenWithStatus(@Param("a") Long a, @Param("b") Long b, @Param("status") FriendshipStatus status);

    List<Friendship> findAllByAddresseeIdAndStatusOrderByCreatedAtDesc(Long addresseeId, FriendshipStatus status);

    @Query("""
            select f from Friendship f
            where f.status = :status and (f.requesterId = :userId or f.addresseeId = :userId)
            """)
    List<Friendship> findAllOfUserWithStatus(@Param("userId") Long userId, @Param("status") FriendshipStatus status);

    @Modifying
    @Query("delete from Friendship f where f.requesterId = :userId or f.addresseeId = :userId")
    void deleteAllOfUser(@Param("userId") Long userId);
}
