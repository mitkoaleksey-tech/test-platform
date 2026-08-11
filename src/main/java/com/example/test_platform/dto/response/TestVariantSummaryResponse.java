package com.example.test_platform.dto.response;

import com.example.test_platform.domain.entity.TestVariant;
import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TestVariantSummaryResponse {

    private final Long id;
    private final String title;
    private final String accessToken;
    private final String shareableUrl;
    private final ExamType examType;
    private final Subject subject;
    private final TaskBank taskBank;
    private final int taskCount;
    private final int attemptsCount;
    private final int studentAttemptsCount;
    private final Double averageScorePercent;
    private final LocalDateTime createdAt;

    public static TestVariantSummaryResponse from(
            TestVariant variant,
            int attemptsCount,
            Double averageScorePercent
    ) {
        return TestVariantSummaryResponse.builder()
                .id(variant.getId())
                .title(variant.getTitle())
                .accessToken(variant.getAccessToken())
                .shareableUrl("/test/" + variant.getAccessToken())
                .examType(variant.getExamType())
                .subject(variant.getSubject())
                .taskBank(variant.getTaskBank())
                .taskCount(variant.getVariantTasks().size())
                .attemptsCount(attemptsCount)
                .studentAttemptsCount(attemptsCount)
                .averageScorePercent(averageScorePercent)
                .createdAt(variant.getCreatedAt())
                .build();
    }
}
