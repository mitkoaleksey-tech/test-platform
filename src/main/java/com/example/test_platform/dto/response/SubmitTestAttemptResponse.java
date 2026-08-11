package com.example.test_platform.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SubmitTestAttemptResponse {

    private final Long attemptId;
    private final String studentName;
    private final LocalDateTime completedAt;
    private final int totalTasks;
    private final int gradableTasks;
    private final int correctCount;
    private final Integer scorePercent;
    private final List<StudentAnswerFeedbackDto> feedback;
}
