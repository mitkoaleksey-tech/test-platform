package com.example.test_platform.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServerMetricsResponse {
    private final double cpuUsagePercent;
    private final long totalMemoryMb;
    private final long freeMemoryMb;
    private final long usedMemoryMb;
    private final long totalDiskGb;
    private final long freeDiskGb;
    private final long usedDiskGb;
    private final long uptimeSeconds;
    private final long systemUptimeSeconds;
    private final int activeConnections;
    private final int activeConnectionsCount;
    private final String dockerStatus;
    private final String appStatus;
}
