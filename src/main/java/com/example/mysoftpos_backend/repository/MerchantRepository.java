package com.example.mysoftpos_backend.repository;

import com.example.mysoftpos_backend.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    List<Merchant> findByAdminId(Long adminId);
    Optional<Merchant> findByMerchantCode(String merchantCode);
    boolean existsByMerchantCode(String merchantCode);
}
