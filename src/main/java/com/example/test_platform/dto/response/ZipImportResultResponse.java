package com.example.test_platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZipImportResultResponse {

    private int totalProcessed;
    private int createdCount;
    private int updatedCount;
    private int imagesAttachedCount;

    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
