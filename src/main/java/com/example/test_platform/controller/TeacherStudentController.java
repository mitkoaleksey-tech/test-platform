package com.example.test_platform.controller;

import com.example.test_platform.domain.entity.Student;
import com.example.test_platform.domain.entity.TestAttempt;
import com.example.test_platform.dto.response.TeacherStudentResponse;
import com.example.test_platform.repository.StudentRepository;
import com.example.test_platform.repository.TestAttemptRepository;
import com.example.test_platform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/teacher/students")
@RequiredArgsConstructor
public class TeacherStudentController {

    private final StudentRepository studentRepository;
    private final TestAttemptRepository testAttemptRepository;

    @GetMapping
    public List<TeacherStudentResponse> getTeacherStudents() {
        Long teacherId = SecurityUtils.getCurrentUser().getId();
        List<Student> students = studentRepository.findByTeacherIdOrderByDisplayNameAsc(teacherId);

        List<TeacherStudentResponse> result = new ArrayList<>();
        for (Student s : students) {
            List<TestAttempt> attempts = testAttemptRepository.findByStudentIdOrderByStartedAtDesc(s.getId());
            int totalAttempts = attempts.size();

            Double avgScore = null;
            List<Integer> scores = attempts.stream()
                    .map(TestAttempt::getScorePercent)
                    .filter(Objects::nonNull)
                    .toList();

            if (!scores.isEmpty()) {
                double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                avgScore = Math.round(avg * 10.0) / 10.0;
            }

            LocalDateTime lastActivity = attempts.isEmpty() ? s.getCreatedAt() : attempts.get(0).getStartedAt();

            result.add(TeacherStudentResponse.builder()
                    .id(s.getId())
                    .displayName(s.getDisplayName())
                    .browserFingerprint(s.getBrowserFingerprint())
                    .totalAttempts(totalAttempts)
                    .averageScorePercent(avgScore)
                    .lastActivityAt(lastActivity)
                    .lastActivityDate(lastActivity)
                    .build());
        }

        return result;
    }
}
