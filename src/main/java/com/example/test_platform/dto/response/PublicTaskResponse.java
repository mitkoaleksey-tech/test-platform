package com.example.test_platform.dto.response;

import com.example.test_platform.config.StorageProperties;
import com.example.test_platform.domain.entity.Task;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PublicTaskResponse {

    private final Long id;
    private final String publicId;
    private final Integer taskNumber;
    private final String subtopic;
    private final String content;
    private final List<TaskImageResponse> images;

    public static PublicTaskResponse from(Task task, StorageProperties storageProperties) {
        return PublicTaskResponse.builder()
                .id(task.getId())
                .publicId(task.getPublicId())
                .taskNumber(task.getTaskNumber())
                .subtopic(task.getSubtopic())
                .content(task.getContent())
                .images(task.getImages().stream()
                        .map(image -> TaskImageResponse.from(image, storageProperties))
                        .toList())
                .build();
    }
}
