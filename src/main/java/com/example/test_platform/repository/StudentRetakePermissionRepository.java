package com.example.test_platform.repository;

import com.example.test_platform.domain.entity.StudentRetakePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRetakePermissionRepository extends JpaRepository<StudentRetakePermission, Long> {
    Optional<StudentRetakePermission> findByTestVariantIdAndStudentId(Long testVariantId, Long studentId);

    void deleteByTestVariantId(Long testVariantId);

    void deleteByStudentId(Long studentId);
}
