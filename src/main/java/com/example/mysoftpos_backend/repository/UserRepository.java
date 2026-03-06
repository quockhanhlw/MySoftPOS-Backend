package com.example.mysoftpos_backend.repository;

import com.example.mysoftpos_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    List<User> findByAdminId(Long adminId);

    List<User> findByAdminIdAndRole(Long adminId, String role);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.lastActiveAt = :lastActiveAt WHERE u.id = :id")
    void updateLastActiveAt(@Param("id") Long id, @Param("lastActiveAt") LocalDateTime lastActiveAt);
}
