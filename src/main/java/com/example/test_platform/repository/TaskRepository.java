package com.example.test_platform.repository;

import com.example.test_platform.domain.entity.Task;
import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByPublicId(String publicId);

    boolean existsByPublicId(String publicId);

    Optional<Task> findByTaskBankAndExternalId(TaskBank taskBank, String externalId);

    boolean existsByTaskBankAndExternalId(TaskBank taskBank, String externalId);

    @org.springframework.data.jpa.repository.Query("SELECT t.externalId FROM Task t WHERE t.taskBank = :taskBank AND t.externalId IS NOT NULL")
    List<String> findExternalIdsByTaskBank(@org.springframework.data.repository.query.Param("taskBank") TaskBank taskBank);

    List<Task> findByActiveTrueAndSubjectAndExamTypeAndTaskBankAndTaskNumberOrderByCreatedAtDesc(
            Subject subject,
            ExamType examType,
            TaskBank taskBank,
            Integer taskNumber
    );

    List<Task> findByActiveTrueOrderByCreatedAtDesc();

    long countBySubject(Subject subject);

    long countByExamType(ExamType examType);

    long countByTaskBank(TaskBank taskBank);

    List<Task> findAllByOrderByCreatedAtDesc();

    List<Task> findAllByOrderByIdDesc();
}
