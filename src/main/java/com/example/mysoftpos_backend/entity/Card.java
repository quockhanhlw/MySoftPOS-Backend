package com.example.mysoftpos_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pan_masked", nullable = false, length = 25)
    private String panMasked;

    @Column(length = 12)
    private String bin;

    @Column(length = 4)
    private String last4;

    @Column(length = 30)
    private String scheme;

    @Column(name = "admin_id", nullable = false)
    @Builder.Default
    private Long adminId = 0L;

    @Column(name = "pos_account_id", nullable = false)
    @Builder.Default
    private Long posAccountId = 0L;
}

