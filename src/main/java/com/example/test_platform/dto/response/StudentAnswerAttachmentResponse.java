package com.example.test_platform.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentAnswerAttachmentResponse {
    private final Long id;
    private final String originalFilename;
    private final String fileUrl;
    private final String contentType;
    @com.fasterxml.jackson.annotation.JsonProperty("isImage")
    private final boolean isImage;
}
