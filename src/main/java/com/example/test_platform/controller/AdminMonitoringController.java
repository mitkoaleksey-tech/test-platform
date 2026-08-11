package com.example.test_platform.controller;

import com.example.test_platform.dto.response.ServerMetricsResponse;
import com.example.test_platform.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/monitoring")
@RequiredArgsConstructor
public class AdminMonitoringController {

    private final MonitoringService monitoringService;

    @GetMapping
    public ServerMetricsResponse getMetrics() {
        return monitoringService.getServerMetrics();
    }
}
