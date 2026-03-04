package com.example.mysoftpos_backend.repository;

import com.example.mysoftpos_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByAdminId(Long adminId);
    List<User> findByAdminIdAndRole(Long adminId, String role);
    List<User> findByAdminIdAndUsernameContainingIgnoreCase(Long adminId, String keyword);
}
