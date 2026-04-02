package com.example.mysoftpos_backend.repository;

import com.example.mysoftpos_backend.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByAdminIdOrderByIdDesc(Long adminId);
    List<Card> findByAdminIdInOrderByIdDesc(Collection<Long> adminIds);
    Optional<Card> findByAdminIdAndPanMasked(Long adminId, String panMasked);
}

