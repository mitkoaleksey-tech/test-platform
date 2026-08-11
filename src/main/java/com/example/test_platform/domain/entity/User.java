package com.example.test_platform.domain.entity;

import com.example.test_platform.domain.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String login;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private boolean temporaryPassword = false;

    private String rawTemporaryPassword;

    @Column(nullable = false)
    private int testsCreatedCount = 0;

    private LocalDateTime lastLoginAt;

    private LocalDateTime subscriptionPaidAt;

    private LocalDateTime nextPaymentAt;
}
