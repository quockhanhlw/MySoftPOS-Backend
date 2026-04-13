package com.example.mysoftpos_backend.repository;

import com.example.mysoftpos_backend.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Merchant m SET m.ownerUserId = null WHERE m.ownerUserId = :ownerUserId")
    int clearOwnerUserId(@Param("ownerUserId") Long ownerUserId);
}
