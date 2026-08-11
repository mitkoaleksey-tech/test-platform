package com.example.test_platform.controller;

import com.example.test_platform.dto.request.CreateTestVariantRequest;
import com.example.test_platform.dto.request.GradeAttemptRequest;
import com.example.test_platform.dto.request.UpdateTestVariantRequest;
import com.example.test_platform.dto.response.AttemptGradingDetailsResponse;
import com.example.test_platform.dto.response.TestVariantResponse;
import com.example.test_platform.dto.response.TestVariantStatsResponse;
import com.example.test_platform.dto.response.TestVariantSummaryResponse;
import com.example.test_platform.service.TestVariantService;
import com.example.test_platform.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/variants")
@RequiredArgsConstructor
public class TeacherTestVariantController {

    private final TestVariantService testVariantService;

    @GetMapping
    public List<TestVariantSummaryResponse> getMyVariants() {
        Long teacherId = SecurityUtils.getCurrentUser().getId();
        return testVariantService.getTeacherVariants(teacherId);
    }

    @GetMapping("/{id}")
    public TestVariantResponse getVariant(@PathVariable Long id) {
        Long teacherId = SecurityUtils.getCurrentUser().getId();
        return testVariantService.getTeacherVariant(id, teacherId);
    }

    /**
     * Returns variant details (tasks with content) for browser-side print rendering.
     */
    @GetMapping("/{id}/print-data")
    public TestVariantResponse getPrintData(@PathVariable Long id) {
        Long teacherId = SecurityUtils.getCurrentUser().getId();
        return testVariantService.getTeacherVariant(id, teacherId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestVariantResponse createVariant(@Valid @RequestBody CreateTestVariantRequest request) {
        Long teacherId = SecurityUtils.getCurrentUser().getId();
        return testVariantService.createVariant(request, teacherId);
    }

    @PutMapping("/{id}")
    public TestVariantResponse updateVariant(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTestVariantRequest request
    ) {
        Long teacherId = SecurityUtils.getCurrentUser().getId();
        return testVariantService.updateVariant(id, request, teacherId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVariant(@PathVariable Long id) {
        Long teacherId = SecurityUtils.getCurrentUser().getId();
        testVariantService.deleteVariant(id, teacherId);
    }

    @GetMapping("/{id}/stats")
    public TestVariantStatsResponse getVariantStats(@PathVariable Long id) {
        Long teacherId = SecurityUtils.getCurrentUser().getId();
        return testVariantService.getVariantStats(id, teacherId);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadVariantPdf(@PathVariable Long id) {
        Long teacherId = SecurityUtils.getCurrentUser().getId();
        byte[] pdfContent = testVariantService.generateVariantPdf(id, teacherId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("variant-" + id + ".pdf")
                        .build()
        );

        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }

    @PostMapping("/{id}/students/{studentId}/allow-retake")
    public ResponseEntity<Map<String, Boolean>> allowStudentRetake(
            @PathVariable Long id,
            @PathVariable Long studentId
    ) {
        Long teacherId = SecurityUtils.getCurrentUser().getId();
        testVariantService.allowStudentRetake(id, studentId, teacherId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/{id}/attempts/{attemptId}")
    public AttemptGradingDetailsResponse getAttemptDetails(
            @PathVariable Long id,
            @PathVariable Long attemptId
    ) {
        Long teacherId = SecurityUtils.getCurrentUser().getId();
        return testVariantService.getAttemptDetails(id, attemptId, teacherId);
    }

    @PostMapping("/{id}/attempts/{attemptId}/grade")
    public ResponseEntity<Map<String, Boolean>> gradeAttempt(
            @PathVariable Long id,
            @PathVariable Long attemptId,
            @RequestBody GradeAttemptRequest request
    ) {
        Long teacherId = SecurityUtils.getCurrentUser().getId();
        testVariantService.gradeStudentAttempt(id, attemptId, teacherId, request);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
