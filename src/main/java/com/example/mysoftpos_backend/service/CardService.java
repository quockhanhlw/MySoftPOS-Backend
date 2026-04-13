package com.example.mysoftpos_backend.service;

import com.example.mysoftpos_backend.dto.CardDto;
import com.example.mysoftpos_backend.entity.Card;
import com.example.mysoftpos_backend.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;

    public List<CardDto> getCardsByAdmin(Long adminId) {
        return cardRepository.findByAdminIdInOrderByIdDesc(Arrays.asList(0L, adminId)).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public int syncCards(Long adminId, Long posAccountId, List<CardDto> cards) {
        Long ownerAdminId = adminId != null ? adminId : 0L;
        if (cards == null || cards.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (CardDto dto : cards) {
            try {
                if (dto == null || dto.getPanMasked() == null || dto.getPanMasked().trim().isEmpty()) {
                    continue;
                }
                Card card = null;
                if (dto.getId() != null) {
                    card = cardRepository.findById(dto.getId()).orElse(null);
                }
                if (card == null) {
                    card = cardRepository.findByAdminIdAndPanMasked(ownerAdminId, dto.getPanMasked()).orElse(null);
                }
                if (card == null) {
                    card = new Card();
                }

                card.setPanMasked(dto.getPanMasked());
                card.setBin(dto.getBin());
                card.setLast4(dto.getLast4());
                card.setScheme(dto.getScheme());
                card.setAdminId(ownerAdminId);
                card.setPosAccountId(posAccountId != null ? posAccountId : 0L);
                cardRepository.save(card);
                count++;
            } catch (Exception ex) {
                log.warn("Skip card sync item adminId={} posAccountId={} panMasked={} reason={}",
                        ownerAdminId,
                        posAccountId,
                        dto != null ? dto.getPanMasked() : null,
                        ex.getMessage());
            }
        }
        return count;
    }

    private CardDto toDto(Card card) {
        return CardDto.builder()
                .id(card.getId())
                .panMasked(card.getPanMasked())
                .bin(card.getBin())
                .last4(card.getLast4())
                .scheme(card.getScheme())
                .adminId(card.getAdminId())
                .posAccountId(card.getPosAccountId())
                .build();
    }
}

