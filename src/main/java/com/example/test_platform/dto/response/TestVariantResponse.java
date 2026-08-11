package com.example.test_platform.dto.response;

import com.example.test_platform.config.StorageProperties;
import com.example.test_platform.domain.entity.TestVariant;
import com.example.test_platform.domain.entity.TestVariantTask;
import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Getter
@Builder
public class TestVariantResponse {

    private final Long id;
    private final String title;
    private final String accessToken;
    private final String shareableUrl;
    private final ExamType examType;
    private final Subject subject;
    private final TaskBank taskBank;
    private final List<TaskResponse> tasks;
    private final int totalTasks;
    private final int attemptsCount;
    private final Double averageScorePercent;
    private final LocalDateTime createdAt;

    public static TestVariantResponse from(
            TestVariant variant,
            StorageProperties storageProperties,
            int attemptsCount,
            Double averageScorePercent
    ) {
        List<TaskResponse> tasks = variant.getVariantTasks().stream()
                .sorted(Comparator.comparingInt(TestVariantTask::getSortOrder))
                .map(vt -> TaskResponse.from(vt.getTask(), storageProperties))
                .toList();

        return TestVariantResponse.builder()
                .id(variant.getId())
                .title(variant.getTitle())
                .accessToken(variant.getAccessToken())
                .shareableUrl("/test/" + variant.getAccessToken())
                .examType(variant.getExamType())
                .subject(variant.getSubject())
                .taskBank(variant.getTaskBank())
                .tasks(tasks)
                .totalTasks(tasks.size())
                .attemptsCount(attemptsCount)
                .averageScorePercent(averageScorePercent)
                .createdAt(variant.getCreatedAt())
                .build();
    }
}
