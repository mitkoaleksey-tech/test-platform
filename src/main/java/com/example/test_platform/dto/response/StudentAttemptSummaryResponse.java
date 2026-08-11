package com.example.test_platform.dto.response;

import com.example.test_platform.domain.entity.TestAttempt;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StudentAttemptSummaryResponse {

    private final Long attemptId;
    private final Long studentId;
    private final String studentName;
    private final LocalDateTime startedAt;
    private final LocalDateTime completedAt;
    private final Integer scorePercent;

    public static StudentAttemptSummaryResponse from(TestAttempt attempt) {
        return StudentAttemptSummaryResponse.builder()
                .attemptId(attempt.getId())
                .studentId(attempt.getStudent().getId())
                .studentName(attempt.getStudent().getDisplayName())
                .startedAt(attempt.getStartedAt())
                .completedAt(attempt.getCompletedAt())
                .scorePercent(attempt.getScorePercent())
                .build();
    }
}
