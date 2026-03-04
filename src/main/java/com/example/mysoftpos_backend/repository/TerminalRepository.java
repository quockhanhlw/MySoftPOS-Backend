package com.example.mysoftpos_backend.repository;

import com.example.mysoftpos_backend.entity.Terminal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TerminalRepository extends JpaRepository<Terminal, Long> {
    List<Terminal> findByMerchantId(Long merchantId);
    List<Terminal> findByMerchantAdminId(Long adminId);
    Optional<Terminal> findByTerminalCode(String terminalCode);
    boolean existsByTerminalCode(String terminalCode);
}
