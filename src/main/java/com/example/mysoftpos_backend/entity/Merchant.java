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

    /** Admin who manages this merchant (nullable for self-registered merchant users) */
    @Column
    private Long adminId;

    /** USER account that owns this merchant profile */
    @Column(name = "owner_user_id", unique = true)
    private Long ownerUserId;

    @Column(length = 4)
    private String businessType;

    @Column(length = 255)
    private String storeAddress;

    @Column
    private Integer branchCount;

    @Column(length = 1000)
    private String branchAddresses;

    @Column
    private Integer accountCount;
}
