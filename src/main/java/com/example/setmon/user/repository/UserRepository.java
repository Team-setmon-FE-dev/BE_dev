package com.example.setmon.user.repository;

import com.example.setmon.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    boolean existsByFriendCode(String friendCode);

    Optional<User> findByFriendCode(String friendCode);
}
