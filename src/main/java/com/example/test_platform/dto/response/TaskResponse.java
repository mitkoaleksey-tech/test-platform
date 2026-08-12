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
    private final String taskVariant;
    private final String topic;
    private final String subtopic;
    private final String taskType;
    private final String content;
    private final String correctAnswer;
    private final boolean active;
    private final boolean hasDetailedAnswer;
    private final List<TaskImageResponse> images;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static TaskResponse from(Task task, StorageProperties storageProperties) {
        if (task == null) return null;
        List<TaskImageResponse> imageResponses = List.of();
        try {
            if (task.getImages() != null && org.hibernate.Hibernate.isInitialized(task.getImages())) {
                imageResponses = task.getImages().stream()
                        .filter(img -> img != null)
                        .map(image -> TaskImageResponse.from(image, storageProperties))
                        .toList();
            }
        } catch (Exception ignored) {}

        LocalDateTime now = LocalDateTime.now();
        return TaskResponse.builder()
                .id(task.getId())
                .publicId(task.getPublicId() != null ? task.getPublicId() : "T-" + task.getId())
                .subject(task.getSubject())
                .taskBank(task.getTaskBank())
                .examType(task.getExamType())
                .taskNumber(task.getTaskNumber() != null ? task.getTaskNumber() : 0)
                .taskVariant(task.getTaskVariant() != null ? task.getTaskVariant() : "")
                .topic(task.getTopic() != null ? task.getTopic() : "")
                .subtopic(task.getSubtopic() != null ? task.getSubtopic() : "")
                .taskType(task.getTaskType() != null ? task.getTaskType() : "")
                .content(task.getContent() != null ? task.getContent() : "")
                .correctAnswer(task.getCorrectAnswer() != null ? task.getCorrectAnswer() : "")
                .active(task.isActive())
                .hasDetailedAnswer(task.isHasDetailedAnswer())
                .images(imageResponses)
                .createdAt(task.getCreatedAt() != null ? task.getCreatedAt() : now)
                .updatedAt(task.getUpdatedAt() != null ? task.getUpdatedAt() : now)
                .build();
    }
}
