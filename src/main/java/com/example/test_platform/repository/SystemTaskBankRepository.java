package com.example.test_platform.repository;

import com.example.test_platform.domain.entity.SystemTaskBank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemTaskBankRepository extends JpaRepository<SystemTaskBank, Long> {
    Optional<SystemTaskBank> findByName(String name);
    boolean existsByName(String name);
}
