package com.example.test_platform.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AttemptGradingDetailsResponse {

    private final Long attemptId;
    private final Long studentId;
    private final String studentName;
    private final String variantTitle;
    private final LocalDateTime startedAt;
    private final LocalDateTime completedAt;
    private final Integer scorePercent;
    private final List<StudentAnswerDetailDto> answers;

    @Getter
    @Builder
    public static class StudentAnswerDetailDto {
        private final Long taskId;
        private final String publicId;
        private final int itemIndex;
        private final int taskNumber;
        private final String subtopic;
        private final String content;
        private final String givenAnswer;
        private final String correctAnswer;
        private final Boolean isCorrect;
        private final List<String> imageUrls;
        private final Integer maxScore;
        private final Integer manualScore;
    }
}
