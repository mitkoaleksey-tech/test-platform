package com.example.test_platform.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TestVariantStatsResponse {

    private final Long variantId;
    private final String title;
    private final String accessToken;
    private final int totalTasks;
    private final int attemptsCount;
    private final Double averageScorePercent;
    private final List<StudentAttemptSummaryResponse> attempts;
}
