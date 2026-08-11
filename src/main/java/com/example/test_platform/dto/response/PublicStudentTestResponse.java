package com.example.test_platform.dto.response;

import com.example.test_platform.config.StorageProperties;
import com.example.test_platform.domain.entity.TestVariant;
import com.example.test_platform.domain.entity.TestVariantTask;
import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import lombok.Builder;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;

@Getter
@Builder
public class PublicStudentTestResponse {

    private final Long variantId;
    private final String title;
    private final String accessToken;
    private final ExamType examType;
    private final Subject subject;
    private final TaskBank taskBank;
    private final int totalTasks;
    private final List<PublicTaskResponse> tasks;

    public static PublicStudentTestResponse from(TestVariant variant, StorageProperties storageProperties) {
        List<PublicTaskResponse> tasks = variant.getVariantTasks().stream()
                .sorted(Comparator.comparingInt(TestVariantTask::getSortOrder))
                .map(vt -> PublicTaskResponse.from(vt.getTask(), storageProperties))
                .toList();

        return PublicStudentTestResponse.builder()
                .variantId(variant.getId())
                .title(variant.getTitle())
                .accessToken(variant.getAccessToken())
                .examType(variant.getExamType())
                .subject(variant.getSubject())
                .taskBank(variant.getTaskBank())
                .totalTasks(tasks.size())
                .tasks(tasks)
                .build();
    }
}
