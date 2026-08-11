package com.example.test_platform.repository;

import com.example.test_platform.domain.entity.TestVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestVariantRepository extends JpaRepository<TestVariant, Long> {

    List<TestVariant> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    Optional<TestVariant> findByAccessToken(String accessToken);

    boolean existsByAccessToken(String accessToken);
}
