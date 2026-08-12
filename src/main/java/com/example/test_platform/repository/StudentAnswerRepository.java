package com.example.test_platform.repository;

import com.example.test_platform.domain.entity.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {

    List<StudentAnswer> findByAttemptIdOrderByTaskIdAsc(Long attemptId);

    java.util.Optional<StudentAnswer> findByAttemptIdAndTaskId(Long attemptId, Long taskId);

    void deleteByAttemptId(Long attemptId);

    void deleteByTaskId(Long taskId);
}
