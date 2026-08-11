package com.example.test_platform.repository;

import com.example.test_platform.domain.entity.SystemSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemSubjectRepository extends JpaRepository<SystemSubject, Long> {
    Optional<SystemSubject> findByName(String name);
    boolean existsByName(String name);
}
