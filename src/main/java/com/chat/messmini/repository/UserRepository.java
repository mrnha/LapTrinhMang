package com.chat.messmini.repository;

import com.chat.messmini.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByUsernameContainingOrDisplayNameContaining(String username, String displayName);
    List<User> findByOnlineTrue();
}
