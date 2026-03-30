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

    @Column(name = "merchant_name", length = 100)
    private String merchantName; // DE 43

    // Merchant profile/contact info from registration form.
    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "phone", length = 20, unique = true)
    private String phone;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    @Column(name = "dob", length = 20)
    private String dob;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "bank_name", length = 22)
    private String bankName;

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

}
