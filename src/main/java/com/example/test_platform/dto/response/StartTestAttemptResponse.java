package com.example.test_platform.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StartTestAttemptResponse {

    private final Long attemptId;
    private final Long studentId;
    private final String studentName;
    private final LocalDateTime startedAt;
}
