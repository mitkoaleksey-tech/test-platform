package com.example.test_platform.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeacherStudentResponse {
    private final Long id;
    private final String displayName;
    private final String browserFingerprint;
    private final int totalAttempts;
    private final Double averageScorePercent;
    private final LocalDateTime lastActivityAt;
    private final LocalDateTime lastActivityDate;
}
