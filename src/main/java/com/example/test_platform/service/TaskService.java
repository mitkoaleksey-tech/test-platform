package com.example.test_platform.service;

import com.example.test_platform.config.StorageProperties;
import com.example.test_platform.domain.entity.Task;
import com.example.test_platform.domain.entity.TaskImage;
import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import com.example.test_platform.dto.request.CreateTaskRequest;
import com.example.test_platform.dto.request.UpdateTaskRequest;
import com.example.test_platform.dto.response.TaskImageResponse;
import com.example.test_platform.dto.response.TaskResponse;
import com.example.test_platform.exception.ApiException;
import com.example.test_platform.repository.TaskRepository;
import com.example.test_platform.util.TaskPublicIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ImageStorageService imageStorageService;
    private final StorageProperties storageProperties;
    private final com.example.test_platform.repository.StudentAnswerRepository studentAnswerRepository;
    private final com.example.test_platform.repository.TestVariantTaskRepository testVariantTaskRepository;

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(
            Subject subject,
            ExamType examType,
            TaskBank taskBank,
            Integer taskNumber,
            Boolean active
    ) {
        return filterTasks(subject, examType, taskBank, taskNumber, active).stream()
                .map(task -> TaskResponse.from(task, storageProperties))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long id) {
        return TaskResponse.from(getTaskEntity(id), storageProperties);
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        Task task = new Task();
        task.setPublicId(generateUniquePublicId());
        task.setExternalId(request.getExternalId());
        applyTaskData(task, request.getSubject(), request.getTaskBank(), request.getExamType(),
                request.getTaskNumber(), request.getSubtopic(), request.getContent(),
                request.getCorrectAnswer(), true, Boolean.TRUE.equals(request.getHasDetailedAnswer()));

        return TaskResponse.from(taskRepository.save(task), storageProperties);
    }

    @Transactional
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        Task task = getTaskEntity(id);
        applyTaskData(task, request.getSubject(), request.getTaskBank(), request.getExamType(),
                request.getTaskNumber(), request.getSubtopic(), request.getContent(),
                request.getCorrectAnswer(), request.isActive(),
                request.getHasDetailedAnswer() != null ? request.getHasDetailedAnswer() : task.isHasDetailedAnswer());

        return TaskResponse.from(taskRepository.save(task), storageProperties);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = getTaskEntity(id);
        imageStorageService.deleteAllTaskImages(task);
        studentAnswerRepository.deleteByTaskId(id);
        testVariantTaskRepository.deleteByTaskId(id);
        taskRepository.delete(task);
    }

    @Transactional
    public TaskImageResponse uploadImage(Long taskId, MultipartFile file) {
        Task task = getTaskEntity(taskId);
        TaskImage image = imageStorageService.storeTaskImage(task, file);
        return TaskImageResponse.from(image, storageProperties);
    }

    @Transactional
    public void deleteImage(Long taskId, Long imageId) {
        Task task = getTaskEntity(taskId);
        TaskImage image = task.getImages().stream()
                .filter(taskImage -> taskImage.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Image not found"));

        task.removeImage(image);
        imageStorageService.deleteTaskImage(image);
    }

    private Task getTaskEntity(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private List<Task> filterTasks(
            Subject subject,
            ExamType examType,
            TaskBank taskBank,
            Integer taskNumber,
            Boolean active
    ) {
        List<Task> tasks = taskRepository.findAllByOrderByIdDesc();

        return tasks.stream()
                .filter(task -> subject == null || task.getSubject() == subject)
                .filter(task -> examType == null || task.getExamType() == examType)
                .filter(task -> taskBank == null || task.getTaskBank() == taskBank)
                .filter(task -> taskNumber == null || (task.getTaskNumber() != null && task.getTaskNumber().equals(taskNumber)))
                .filter(task -> active == null || task.isActive() == active)
                .toList();
    }

    private String generateUniquePublicId() {
        String publicId;
        do {
            publicId = TaskPublicIdGenerator.generate();
        } while (taskRepository.existsByPublicId(publicId));
        return publicId;
    }

    private void applyTaskData(
            Task task,
            Subject subject,
            TaskBank taskBank,
            ExamType examType,
            Integer taskNumber,
            String subtopic,
            String content,
            String correctAnswer,
            boolean active,
            boolean hasDetailedAnswer
    ) {
        task.setSubject(subject);
        task.setTaskBank(taskBank);
        task.setExamType(examType);
        task.setTaskNumber(taskNumber);
        task.setSubtopic(subtopic != null ? subtopic.trim() : "");
        task.setContent(com.example.test_platform.util.FipiTextNormalizer.normalize(content));
        task.setCorrectAnswer(correctAnswer);
        task.setActive(active);
        task.setHasDetailedAnswer(hasDetailedAnswer);
    }
}

