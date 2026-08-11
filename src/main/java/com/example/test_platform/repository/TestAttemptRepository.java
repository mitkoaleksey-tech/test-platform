package com.example.test_platform.repository;

import com.example.test_platform.domain.entity.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {

    List<TestAttempt> findByTestVariantIdOrderByStartedAtDesc(Long testVariantId);

    List<TestAttempt> findByStudentIdOrderByStartedAtDesc(Long studentId);

    Optional<TestAttempt> findByTestVariantIdAndStudentId(Long testVariantId, Long studentId);

    void deleteByTestVariantId(Long testVariantId);

    void deleteByStudentId(Long studentId);
}
