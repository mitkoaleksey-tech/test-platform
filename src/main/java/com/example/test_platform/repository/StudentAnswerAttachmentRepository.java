package com.example.test_platform.repository;

import com.example.test_platform.domain.entity.StudentAnswerAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentAnswerAttachmentRepository extends JpaRepository<StudentAnswerAttachment, Long> {
    List<StudentAnswerAttachment> findByStudentAnswerId(Long studentAnswerId);
}
