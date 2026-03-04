package com.example.mysoftpos_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "terminals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Terminal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String terminalCode; // DE 41

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(length = 50)
    private String serverIp;

    @Column
    private Integer serverPort;
}
