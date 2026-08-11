package com.example.test_platform.repository;

import com.example.test_platform.domain.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByTeacherIdAndDisplayNameIgnoreCaseAndBrowserFingerprint(
            Long teacherId,
            String displayName,
            String browserFingerprint
    );

    List<Student> findByTeacherIdOrderByDisplayNameAsc(Long teacherId);
}
