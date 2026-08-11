package com.example.test_platform.repository;

import com.example.test_platform.domain.entity.KimTaskSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KimTaskSettingRepository extends JpaRepository<KimTaskSetting, Long> {
    List<KimTaskSetting> findBySubjectNameAndExamNameOrderByTaskNumberAsc(String subjectName, String examName);
    Optional<KimTaskSetting> findBySubjectNameAndExamNameAndTaskNumber(String subjectName, String examName, Integer taskNumber);
}
