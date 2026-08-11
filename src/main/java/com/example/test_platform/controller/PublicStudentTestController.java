package com.example.test_platform.controller;

import com.example.test_platform.dto.request.StartTestAttemptRequest;
import com.example.test_platform.dto.request.SubmitTestAttemptRequest;
import com.example.test_platform.dto.response.PublicStudentTestResponse;
import com.example.test_platform.dto.response.StartTestAttemptResponse;
import com.example.test_platform.dto.response.SubmitTestAttemptResponse;
import com.example.test_platform.service.StudentTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/tests")
@RequiredArgsConstructor
public class PublicStudentTestController {

    private final StudentTestService studentTestService;

    @GetMapping("/{accessToken}")
    public PublicStudentTestResponse getPublicTestVariant(@PathVariable String accessToken) {
        return studentTestService.getPublicTestVariant(accessToken);
    }

    @PostMapping("/{accessToken}/start")
    public StartTestAttemptResponse startTestAttempt(
            @PathVariable String accessToken,
            @Valid @RequestBody StartTestAttemptRequest request
    ) {
        return studentTestService.startTestAttempt(accessToken, request);
    }

    @PostMapping("/{accessToken}/submit")
    public SubmitTestAttemptResponse submitTestAttempt(
            @PathVariable String accessToken,
            @Valid @RequestBody SubmitTestAttemptRequest request
    ) {
        return studentTestService.submitTestAttempt(accessToken, request);
    }
}
