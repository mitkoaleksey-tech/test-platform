package com.example.test_platform.dto.response;

import com.example.test_platform.config.StorageProperties;
import com.example.test_platform.domain.entity.Task;
import com.example.test_platform.domain.entity.TaskImage;
import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TaskResponse {

    private final Long id;
    private final String publicId;
    private final Subject subject;
    private final TaskBank taskBank;
    private final ExamType examType;
    private final Integer taskNumber;
    private final String subtopic;
    private final String content;
    private final String correctAnswer;
    private final boolean active;
    private final List<TaskImageResponse> images;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static TaskResponse from(Task task, StorageProperties storageProperties) {
        return TaskResponse.builder()
                .id(task.getId())
                .publicId(task.getPublicId())
                .subject(task.getSubject())
                .taskBank(task.getTaskBank())
                .examType(task.getExamType())
                .taskNumber(task.getTaskNumber())
                .subtopic(task.getSubtopic())
                .content(task.getContent())
                .correctAnswer(task.getCorrectAnswer())
                .active(task.isActive())
                .images(task.getImages().stream()
                        .map(image -> TaskImageResponse.from(image, storageProperties))
                        .toList())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
