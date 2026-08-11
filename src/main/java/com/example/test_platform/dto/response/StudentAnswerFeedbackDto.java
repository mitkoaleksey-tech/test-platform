package com.example.test_platform.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentAnswerFeedbackDto {

    public enum GradingStatus {
        CORRECT,
        INCORRECT,
        UNGRADED
    }

    private final Long taskId;
    private final String publicId;
    private final int itemIndex;
    private final String subtopic;
    private final String givenAnswer;
    private final String correctAnswer;
    private final GradingStatus status;
}
