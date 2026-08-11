package com.example.test_platform.dto.response;

import com.example.test_platform.config.StorageProperties;
import com.example.test_platform.domain.entity.TaskImage;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskImageResponse {

    private final Long id;
    private final String url;
    private final String originalFilename;
    private final int widthPx;
    private final int sortOrder;

    public static TaskImageResponse from(TaskImage image, StorageProperties storageProperties) {
        return TaskImageResponse.builder()
                .id(image.getId())
                .url(storageProperties.getPublicUrlPrefix() + "/" + image.getFilePath().replace("\\", "/"))
                .originalFilename(image.getOriginalFilename())
                .widthPx(image.getWidthPx())
                .sortOrder(image.getSortOrder())
                .build();
    }
}
