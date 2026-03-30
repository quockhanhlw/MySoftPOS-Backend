package com.example.mysoftpos_backend.repository;

import com.example.mysoftpos_backend.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    List<Merchant> findByAdminId(Long adminId);
    Optional<Merchant> findByOwnerUserId(Long ownerUserId);
    Optional<Merchant> findByMerchantCode(String merchantCode);
    Optional<Merchant> findByEmail(String email);
    Optional<Merchant> findByPhone(String phone);
    boolean existsByMerchantCode(String merchantCode);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    void deleteByOwnerUserId(Long ownerUserId);
}
