package com.example.test_platform.repository;

import com.example.test_platform.domain.entity.SystemExamType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemExamTypeRepository extends JpaRepository<SystemExamType, Long> {
    Optional<SystemExamType> findByName(String name);
    boolean existsByName(String name);
}
