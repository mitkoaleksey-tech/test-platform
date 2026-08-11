package com.example.test_platform.controller;

import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import com.example.test_platform.dto.request.CreateTaskRequest;
import com.example.test_platform.dto.request.UpdateTaskRequest;
import com.example.test_platform.dto.response.TaskImageResponse;
import com.example.test_platform.dto.response.TaskResponse;
import com.example.test_platform.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tasks")
@RequiredArgsConstructor
public class AdminTaskController {

    private final TaskService taskService;
    private final com.example.test_platform.service.FipiCsvImporterService fipiCsvImporterService;
    private final com.example.test_platform.service.ZipImportService zipImportService;

    @PostMapping("/import-zip")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public com.example.test_platform.dto.response.ZipImportResultResponse importZip(
            @RequestParam("file") MultipartFile file
    ) {
        return zipImportService.importZipArchive(file);
    }

    @PostMapping("/import-csv")
    public com.example.test_platform.service.FipiCsvImporterService.ImportResult importCsv(
            @RequestParam(defaultValue = "archive/tasks.csv") String csvPath,
            @RequestParam(defaultValue = "archive/assets") String imagesPath
    ) {
        return fipiCsvImporterService.importCsv(new java.io.File(csvPath), new java.io.File(imagesPath));
    }

    @GetMapping
    public List<TaskResponse> getTasks(
            @RequestParam(required = false) Subject subject,
            @RequestParam(required = false) ExamType examType,
            @RequestParam(required = false) TaskBank taskBank,
            @RequestParam(required = false) Integer taskNumber,
            @RequestParam(required = false) Boolean active
    ) {
        return taskService.getTasks(subject, examType, taskBank, taskNumber, active);
    }

    @GetMapping("/{id}")
    public TaskResponse getTask(@PathVariable Long id) {
        return taskService.getTask(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@Valid @RequestBody CreateTaskRequest request) {
        return taskService.createTask(request);
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return taskService.updateTask(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
    }

    @PostMapping("/{id}/images")
    public TaskImageResponse uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        return taskService.uploadImage(id, file);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId
    ) {
        taskService.deleteImage(id, imageId);
    }
}
