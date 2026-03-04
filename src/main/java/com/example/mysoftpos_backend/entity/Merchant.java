package com.example.mysoftpos_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "merchants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 15)
    private String merchantCode; // DE 42

    @Column(length = 100)
    private String merchantName; // DE 43

    /** Admin who owns this merchant */
    @Column(nullable = false)
    private Long adminId;
}
