package com.example.test_platform.service;

import com.example.test_platform.config.StorageProperties;
import com.example.test_platform.domain.entity.Student;
import com.example.test_platform.domain.entity.StudentAnswer;
import com.example.test_platform.domain.entity.Task;
import com.example.test_platform.domain.entity.TestAttempt;
import com.example.test_platform.domain.entity.TestVariant;
import com.example.test_platform.domain.entity.TestVariantTask;
import com.example.test_platform.dto.request.StartTestAttemptRequest;
import com.example.test_platform.dto.request.StudentAnswerDto;
import com.example.test_platform.dto.request.SubmitTestAttemptRequest;
import com.example.test_platform.dto.response.PublicStudentTestResponse;
import com.example.test_platform.dto.response.StartTestAttemptResponse;
import com.example.test_platform.dto.response.StudentAnswerFeedbackDto;
import com.example.test_platform.dto.response.SubmitTestAttemptResponse;
import com.example.test_platform.exception.ApiException;
import com.example.test_platform.repository.StudentAnswerRepository;
import com.example.test_platform.repository.StudentRepository;
import com.example.test_platform.repository.TestAttemptRepository;
import com.example.test_platform.repository.TestVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentTestService {

    private final TestVariantRepository testVariantRepository;
    private final StudentRepository studentRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final com.example.test_platform.repository.StudentAnswerAttachmentRepository studentAnswerAttachmentRepository;
    private final com.example.test_platform.repository.TaskRepository taskRepository;
    private final com.example.test_platform.repository.StudentRetakePermissionRepository studentRetakePermissionRepository;
    private final StorageProperties storageProperties;

    @Transactional(readOnly = true)
    public PublicStudentTestResponse getPublicTestVariant(String accessToken) {
        TestVariant variant = getVariantByAccessToken(accessToken);
        return PublicStudentTestResponse.from(variant, storageProperties);
    }

    @Transactional
    public StartTestAttemptResponse startTestAttempt(String accessToken, StartTestAttemptRequest request) {
        TestVariant variant = getVariantByAccessToken(accessToken);

        String fingerprint = (request.getBrowserFingerprint() == null || request.getBrowserFingerprint().isBlank())
                ? "anonymous-fp"
                : request.getBrowserFingerprint().trim();

        Student student = studentRepository.findByTeacherIdAndDisplayNameIgnoreCaseAndBrowserFingerprint(
                variant.getTeacher().getId(),
                request.getStudentName().trim(),
                fingerprint
        ).orElseGet(() -> {
            Student newStudent = new Student();
            newStudent.setTeacher(variant.getTeacher());
            newStudent.setDisplayName(request.getStudentName().trim());
            newStudent.setBrowserFingerprint(fingerprint);
            return studentRepository.save(newStudent);
        });

        // Check for completed attempts for this variant by student ID OR by device fingerprint
        List<TestAttempt> existingStudentAttempts = testAttemptRepository.findByStudentIdOrderByStartedAtDesc(student.getId()).stream()
                .filter(a -> a.getTestVariant().getId().equals(variant.getId()))
                .toList();

        boolean hasStudentCompleted = existingStudentAttempts.stream().anyMatch(a -> a.getCompletedAt() != null);

        // Check if ANY student on this browser fingerprint completed this test variant
        boolean hasDeviceCompleted = !fingerprint.equals("anonymous-fp") && testAttemptRepository.findAll().stream()
                .filter(a -> a.getTestVariant().getId().equals(variant.getId()))
                .filter(a -> a.getCompletedAt() != null)
                .anyMatch(a -> a.getStudent() != null && fingerprint.equalsIgnoreCase(a.getStudent().getBrowserFingerprint()));

        boolean hasCompleted = hasStudentCompleted || hasDeviceCompleted;

        if (hasCompleted) {
            java.util.Optional<com.example.test_platform.domain.entity.StudentRetakePermission> permissionOpt = 
                    studentRetakePermissionRepository.findByTestVariantIdAndStudentId(variant.getId(), student.getId());
            boolean isRetakeAllowed = permissionOpt.isPresent() && permissionOpt.get().isRetakeAllowed();

            if (!isRetakeAllowed) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Вы уже сдали этот тест на данном устройстве. Повторное прохождение разрешает преподаватель");
            } else {
                com.example.test_platform.domain.entity.StudentRetakePermission permission = permissionOpt.get();
                permission.setRetakeAllowed(false);
                studentRetakePermissionRepository.save(permission);
            }
        }

        TestAttempt attempt = new TestAttempt();
        attempt.setTestVariant(variant);
        attempt.setStudent(student);
        attempt.setStartedAt(LocalDateTime.now());

        TestAttempt saved = testAttemptRepository.save(attempt);

        return StartTestAttemptResponse.builder()
                .attemptId(saved.getId())
                .studentId(student.getId())
                .studentName(student.getDisplayName())
                .startedAt(saved.getStartedAt())
                .build();
    }


    @Transactional
    public SubmitTestAttemptResponse submitTestAttempt(String accessToken, SubmitTestAttemptRequest request) {
        TestVariant variant = getVariantByAccessToken(accessToken);

        TestAttempt attempt = testAttemptRepository.findById(request.getAttemptId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Test attempt not found"));

        if (!attempt.getTestVariant().getId().equals(variant.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt does not belong to this test variant");
        }

        if (attempt.getCompletedAt() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Test attempt has already been submitted");
        }

        Map<Long, String> givenAnswersMap = request.getAnswers().stream()
                .filter(a -> a.getTaskId() != null)
                .collect(Collectors.toMap(
                        StudentAnswerDto::getTaskId,
                        a -> a.getAnswer() == null ? "" : a.getAnswer(),
                        (existing, replacement) -> replacement
                ));

        List<TestVariantTask> variantTasks = variant.getVariantTasks().stream()
                .sorted(Comparator.comparingInt(TestVariantTask::getSortOrder))
                .toList();

        List<StudentAnswerFeedbackDto> feedbackList = new ArrayList<>();
        int gradableTasks = 0;
        int correctCount = 0;

        for (int i = 0; i < variantTasks.size(); i++) {
            TestVariantTask vt = variantTasks.get(i);
            Task task = vt.getTask();

            String givenAnswer = givenAnswersMap.getOrDefault(task.getId(), "").trim();
            String correctAnswer = task.getCorrectAnswer();

            boolean isDetailed = task.isHasDetailedAnswer();
            boolean hasCorrectAnswer = !isDetailed && correctAnswer != null && !correctAnswer.isBlank();

            StudentAnswerFeedbackDto.GradingStatus status;
            boolean isCorrect = false;

            if (isDetailed || !hasCorrectAnswer) {
                status = StudentAnswerFeedbackDto.GradingStatus.UNGRADED;
            } else {
                gradableTasks++;
                isCorrect = isAnswerMatching(givenAnswer, correctAnswer);
                if (isCorrect) {
                    correctCount++;
                    status = StudentAnswerFeedbackDto.GradingStatus.CORRECT;
                } else {
                    status = StudentAnswerFeedbackDto.GradingStatus.INCORRECT;
                }
            }

            StudentAnswer answerEntity = studentAnswerRepository.findByAttemptIdAndTaskId(attempt.getId(), task.getId())
                    .orElseGet(() -> {
                        StudentAnswer sa = new StudentAnswer();
                        sa.setAttempt(attempt);
                        sa.setTask(task);
                        return sa;
                    });
            answerEntity.setAnswerText(givenAnswer);
            answerEntity.setCorrect(isCorrect);
            studentAnswerRepository.save(answerEntity);

            feedbackList.add(StudentAnswerFeedbackDto.builder()
                    .taskId(task.getId())
                    .publicId(task.getPublicId())
                    .itemIndex(i + 1)
                    .subtopic(task.getSubtopic())
                    .givenAnswer(givenAnswer)
                    .correctAnswer(hasCorrectAnswer ? correctAnswer : null)
                    .status(status)
                    .build());
        }

        Integer scorePercent = null;
        if (gradableTasks > 0) {
            scorePercent = (int) Math.round((correctCount * 100.0) / gradableTasks);
        }

        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setScorePercent(scorePercent);
        testAttemptRepository.save(attempt);

        return SubmitTestAttemptResponse.builder()
                .attemptId(attempt.getId())
                .studentName(attempt.getStudent().getDisplayName())
                .completedAt(attempt.getCompletedAt())
                .totalTasks(variantTasks.size())
                .gradableTasks(gradableTasks)
                .correctCount(correctCount)
                .scorePercent(scorePercent)
                .feedback(feedbackList)
                .build();
    }

    @Transactional
    public com.example.test_platform.dto.response.StudentAnswerAttachmentResponse uploadStudentAttachment(
            String accessToken, Long attemptId, Long taskId, org.springframework.web.multipart.MultipartFile file) {
        TestVariant variant = getVariantByAccessToken(accessToken);
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Test attempt not found"));

        if (!attempt.getTestVariant().getId().equals(variant.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt does not belong to this test variant");
        }

        if (attempt.getCompletedAt() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Test attempt has already been submitted");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));

        StudentAnswer sa = studentAnswerRepository.findByAttemptIdAndTaskId(attemptId, taskId)
                .orElseGet(() -> {
                    StudentAnswer newSa = new StudentAnswer();
                    newSa.setAttempt(attempt);
                    newSa.setTask(task);
                    return studentAnswerRepository.save(newSa);
                });

        String originalFilename = file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
                ? file.getOriginalFilename()
                : "attachment";
        String ext = getFileExtension(originalFilename);
        String storedFilename = java.util.UUID.randomUUID() + ext;

        java.nio.file.Path targetDir = java.nio.file.Path.of(storageProperties.getImagesPath(), "student_answers", String.valueOf(attemptId));
        try {
            java.nio.file.Files.createDirectories(targetDir);
            java.nio.file.Path targetPath = targetDir.resolve(storedFilename);
            java.nio.file.Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save attachment file");
        }

        String relativePath = "student_answers/" + attemptId + "/" + storedFilename;
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        com.example.test_platform.domain.entity.StudentAnswerAttachment attachment =
                new com.example.test_platform.domain.entity.StudentAnswerAttachment();
        attachment.setFilePath(relativePath);
        attachment.setOriginalFilename(originalFilename);
        attachment.setContentType(contentType);
        sa.addAttachment(attachment);

        com.example.test_platform.domain.entity.StudentAnswerAttachment saved =
                studentAnswerAttachmentRepository.save(attachment);

        boolean isImage = contentType.startsWith("image/") || isImageExtension(ext);
        String fileUrl = "/api/public/attachments/" + relativePath;

        return com.example.test_platform.dto.response.StudentAnswerAttachmentResponse.builder()
                .id(saved.getId())
                .originalFilename(originalFilename)
                .fileUrl(fileUrl)
                .contentType(contentType)
                .isImage(isImage)
                .build();
    }

    @Transactional
    public void deleteStudentAttachment(String accessToken, Long attemptId, Long taskId, Long attachmentId) {
        TestVariant variant = getVariantByAccessToken(accessToken);
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Test attempt not found"));

        if (!attempt.getTestVariant().getId().equals(variant.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt does not belong to this test variant");
        }

        if (attempt.getCompletedAt() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Test attempt has already been submitted");
        }

        com.example.test_platform.domain.entity.StudentAnswerAttachment attachment =
                studentAnswerAttachmentRepository.findById(attachmentId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attachment not found"));

        if (!attachment.getStudentAnswer().getAttempt().getId().equals(attemptId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Attachment does not belong to this attempt");
        }

        try {
            java.nio.file.Path filePath = java.nio.file.Path.of(storageProperties.getImagesPath()).resolve(attachment.getFilePath());
            java.nio.file.Files.deleteIfExists(filePath);
        } catch (java.io.IOException ignored) {}

        attachment.getStudentAnswer().removeAttachment(attachment);
        studentAnswerAttachmentRepository.delete(attachment);
    }

    private String getFileExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx) : ".bin";
    }

    private boolean isImageExtension(String ext) {
        String lower = ext.toLowerCase(Locale.ROOT);
        return lower.equals(".png") || lower.equals(".jpg") || lower.equals(".jpeg") || lower.equals(".gif") || lower.equals(".webp");
    }

    private TestVariant getVariantByAccessToken(String accessToken) {
        return testVariantRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Test variant not found for link"));
    }

    private boolean isAnswerMatching(String given, String expected) {
        if (given == null || expected == null) {
            return false;
        }

        String normGiven = normalizeAnswer(given);
        String normExpected = normalizeAnswer(expected);

        return normGiven.equalsIgnoreCase(normExpected);
    }

    private String normalizeAnswer(String text) {
        return text.trim()
                .replace(',', '.')
                .toLowerCase(Locale.ROOT);
    }
}
