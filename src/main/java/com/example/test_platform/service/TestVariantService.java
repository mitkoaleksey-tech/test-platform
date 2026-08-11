package com.example.test_platform.service;

import com.example.test_platform.config.StorageProperties;
import com.example.test_platform.domain.entity.KimTaskSetting;
import com.example.test_platform.domain.entity.Task;
import com.example.test_platform.domain.entity.TestAttempt;
import com.example.test_platform.domain.entity.TestVariant;
import com.example.test_platform.domain.entity.TestVariantTask;
import com.example.test_platform.domain.entity.User;
import com.example.test_platform.dto.request.CreateTestVariantRequest;
import com.example.test_platform.dto.request.GradeAttemptRequest;
import com.example.test_platform.dto.request.UpdateTestVariantRequest;
import com.example.test_platform.dto.response.StudentAttemptSummaryResponse;
import com.example.test_platform.dto.response.TestVariantResponse;
import com.example.test_platform.dto.response.TestVariantStatsResponse;
import com.example.test_platform.dto.response.TestVariantSummaryResponse;
import com.example.test_platform.exception.ApiException;
import com.example.test_platform.repository.KimTaskSettingRepository;
import com.example.test_platform.repository.TaskRepository;
import com.example.test_platform.repository.TestAttemptRepository;
import com.example.test_platform.repository.TestVariantRepository;
import com.example.test_platform.repository.TestVariantTaskRepository;
import com.example.test_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestVariantService {

    private final TestVariantRepository testVariantRepository;
    private final TestVariantTaskRepository testVariantTaskRepository;
    private final TaskRepository taskRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final UserRepository userRepository;
    private final PdfExportService pdfExportService;
    private final StorageProperties storageProperties;
    private final com.example.test_platform.repository.StudentRepository studentRepository;
    private final com.example.test_platform.repository.StudentRetakePermissionRepository studentRetakePermissionRepository;
    private final KimTaskSettingRepository kimTaskSettingRepository;

    @Transactional
    public void allowStudentRetake(Long variantId, Long studentId, Long teacherId) {
        TestVariant variant = getTeacherVariantEntity(variantId, teacherId);
        com.example.test_platform.domain.entity.Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student not found"));

        com.example.test_platform.domain.entity.StudentRetakePermission permission = studentRetakePermissionRepository
                .findByTestVariantIdAndStudentId(variant.getId(), student.getId())
                .orElseGet(() -> {
                    com.example.test_platform.domain.entity.StudentRetakePermission newPerm = new com.example.test_platform.domain.entity.StudentRetakePermission();
                    newPerm.setTestVariant(variant);
                    newPerm.setStudent(student);
                    return newPerm;
                });

        permission.setRetakeAllowed(true);
        studentRetakePermissionRepository.save(permission);
    }



    @Transactional(readOnly = true)
    public List<TestVariantSummaryResponse> getTeacherVariants(Long teacherId) {
        List<TestVariant> variants = testVariantRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId);

        return variants.stream()
                .map(variant -> {
                    List<TestAttempt> attempts = testAttemptRepository.findByTestVariantIdOrderByStartedAtDesc(variant.getId());
                    int attemptsCount = attempts.size();
                    Double avgScore = calculateAverageScore(attempts);
                    return TestVariantSummaryResponse.from(variant, attemptsCount, avgScore);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public TestVariantResponse getTeacherVariant(Long id, Long teacherId) {
        TestVariant variant = getTeacherVariantEntity(id, teacherId);
        List<TestAttempt> attempts = testAttemptRepository.findByTestVariantIdOrderByStartedAtDesc(id);
        return TestVariantResponse.from(variant, storageProperties, attempts.size(), calculateAverageScore(attempts));
    }

    @Transactional
    public TestVariantResponse createVariant(CreateTestVariantRequest request, Long teacherId) {
        validateSubjectAndExam(request.getSubject(), request.getExamType());
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Teacher not found"));
        List<Task> tasks = fetchTasksInOrder(request.getTaskIds());

        TestVariant variant = new TestVariant();
        variant.setTeacher(teacher);
        variant.setTitle(request.getTitle());
        variant.setExamType(request.getExamType());
        variant.setSubject(request.getSubject());
        variant.setTaskBank(request.getTaskBank());
        variant.setAccessToken(generateUniqueAccessToken());

        attachVariantTasks(variant, tasks);

        TestVariant saved = testVariantRepository.save(variant);
        return TestVariantResponse.from(saved, storageProperties, 0, null);
    }

    private void validateSubjectAndExam(com.example.test_platform.domain.enums.Subject subject, com.example.test_platform.domain.enums.ExamType examType) {
        if (subject == null || examType == null) return;
        if (subject == com.example.test_platform.domain.enums.Subject.MATHEMATICS_BASE && examType != com.example.test_platform.domain.enums.ExamType.EGE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Математика (Базовый уровень) доступна только для экзамена ЕГЭ");
        }
        if (subject == com.example.test_platform.domain.enums.Subject.MATHEMATICS_PROF && examType != com.example.test_platform.domain.enums.ExamType.EGE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Математика (Профильный уровень) доступна только для экзамена ЕГЭ");
        }
    }

    @Transactional
    public TestVariantResponse updateVariant(Long id, UpdateTestVariantRequest request, Long teacherId) {
        TestVariant variant = getTeacherVariantEntity(id, teacherId);
        List<Task> tasks = fetchTasksInOrder(request.getTaskIds());

        variant.setTitle(request.getTitle());
        testVariantTaskRepository.deleteByTestVariantId(variant.getId());
        testVariantTaskRepository.flush();
        variant.getVariantTasks().clear();

        attachVariantTasks(variant, tasks);

        TestVariant saved = testVariantRepository.save(variant);
        List<TestAttempt> attempts = testAttemptRepository.findByTestVariantIdOrderByStartedAtDesc(id);
        return TestVariantResponse.from(saved, storageProperties, attempts.size(), calculateAverageScore(attempts));
    }

    private final com.example.test_platform.repository.StudentAnswerRepository studentAnswerRepository;

    @Transactional
    public void deleteVariant(Long id, Long teacherId) {
        TestVariant variant = getTeacherVariantEntity(id, teacherId);
        studentRetakePermissionRepository.deleteByTestVariantId(variant.getId());
        List<TestAttempt> attempts = testAttemptRepository.findByTestVariantIdOrderByStartedAtDesc(variant.getId());
        for (TestAttempt attempt : attempts) {
            studentAnswerRepository.deleteByAttemptId(attempt.getId());
        }
        testAttemptRepository.deleteByTestVariantId(variant.getId());
        testVariantTaskRepository.deleteByTestVariantId(variant.getId());
        testVariantRepository.delete(variant);
    }

    @Transactional(readOnly = true)
    public TestVariantStatsResponse getVariantStats(Long id, Long teacherId) {
        TestVariant variant = getTeacherVariantEntity(id, teacherId);
        List<TestAttempt> attempts = testAttemptRepository.findByTestVariantIdOrderByStartedAtDesc(id);

        List<StudentAttemptSummaryResponse> attemptSummaries = attempts.stream()
                .map(StudentAttemptSummaryResponse::from)
                .toList();

        return TestVariantStatsResponse.builder()
                .variantId(variant.getId())
                .title(variant.getTitle())
                .accessToken(variant.getAccessToken())
                .totalTasks(variant.getVariantTasks().size())
                .attemptsCount(attempts.size())
                .averageScorePercent(calculateAverageScore(attempts))
                .attempts(attemptSummaries)
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] generateVariantPdf(Long id, Long teacherId) {
        TestVariant variant = getTeacherVariantEntity(id, teacherId);
        return pdfExportService.generateTestVariantPdf(variant);
    }

    @Transactional(readOnly = true)
    public com.example.test_platform.dto.response.AttemptGradingDetailsResponse getAttemptDetails(Long variantId, Long attemptId, Long teacherId) {
        TestVariant variant = getTeacherVariantEntity(variantId, teacherId);
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Test attempt not found"));

        if (!attempt.getTestVariant().getId().equals(variant.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt does not belong to this test variant");
        }

        List<com.example.test_platform.domain.entity.StudentAnswer> studentAnswers =
                studentAnswerRepository.findByAttemptIdOrderByTaskIdAsc(attempt.getId());
        Map<Long, com.example.test_platform.domain.entity.StudentAnswer> answerMap = studentAnswers.stream()
                .collect(Collectors.toMap(a -> a.getTask().getId(), a -> a, (e, r) -> r));

        // Build KIM setting lookup: subjectName + examType -> taskNumber -> maxScore
        String subjectName = variant.getSubject() != null ? variant.getSubject().name() : null;
        String examName = variant.getExamType() != null ? variant.getExamType().name() : null;
        Map<Integer, Integer> kimMaxScoreByTaskNumber = new java.util.HashMap<>();
        if (subjectName != null && examName != null) {
            List<KimTaskSetting> kimSettings = kimTaskSettingRepository
                    .findBySubjectNameAndExamNameOrderByTaskNumberAsc(subjectName, examName);
            kimSettings.forEach(s -> kimMaxScoreByTaskNumber.put(s.getTaskNumber(), s.getMaxScore()));
        }

        List<TestVariantTask> variantTasks = variant.getVariantTasks().stream()
                .sorted(java.util.Comparator.comparingInt(TestVariantTask::getSortOrder))
                .toList();

        List<com.example.test_platform.dto.response.AttemptGradingDetailsResponse.StudentAnswerDetailDto> detailDtos = new ArrayList<>();
        for (int i = 0; i < variantTasks.size(); i++) {
            TestVariantTask vt = variantTasks.get(i);
            Task task = vt.getTask();
            com.example.test_platform.domain.entity.StudentAnswer sa = answerMap.get(task.getId());

            int taskNum = task.getTaskNumber() != null ? task.getTaskNumber() : 0;
            Integer maxScore = kimMaxScoreByTaskNumber.getOrDefault(taskNum, 1);

            List<String> imageUrls = task.getImages() != null ? task.getImages().stream()
                    .map(img -> "/api/public/images/" + img.getFilePath())
                    .toList() : List.of();

            detailDtos.add(com.example.test_platform.dto.response.AttemptGradingDetailsResponse.StudentAnswerDetailDto.builder()
                    .taskId(task.getId())
                    .publicId(task.getPublicId())
                    .itemIndex(i + 1)
                    .taskNumber(taskNum)
                    .subtopic(task.getSubtopic())
                    .content(task.getContent())
                    .givenAnswer(sa != null ? sa.getAnswerText() : "")
                    .correctAnswer(task.getCorrectAnswer())
                    .isCorrect(sa != null ? sa.isCorrect() : null)
                    .imageUrls(imageUrls)
                    .maxScore(maxScore)
                    .manualScore(sa != null ? sa.getManualScore() : null)
                    .build());
        }

        return com.example.test_platform.dto.response.AttemptGradingDetailsResponse.builder()
                .attemptId(attempt.getId())
                .studentId(attempt.getStudent().getId())
                .studentName(attempt.getStudent().getDisplayName())
                .variantTitle(variant.getTitle())
                .startedAt(attempt.getStartedAt())
                .completedAt(attempt.getCompletedAt())
                .scorePercent(attempt.getScorePercent())
                .answers(detailDtos)
                .build();
    }

    @Transactional
    public void gradeStudentAttempt(Long variantId, Long attemptId, Long teacherId, GradeAttemptRequest req) {
        TestVariant variant = getTeacherVariantEntity(variantId, teacherId);
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Test attempt not found"));

        if (!attempt.getTestVariant().getId().equals(variant.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attempt does not belong to this test variant");
        }

        if (req == null || req.getTaskGrades() == null || req.getTaskGrades().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Task grades must not be empty");
        }

        // Build KIM max score map
        String subjectName = variant.getSubject() != null ? variant.getSubject().name() : null;
        String examName = variant.getExamType() != null ? variant.getExamType().name() : null;
        Map<Integer, Integer> kimMaxScoreByTaskNumber = new java.util.HashMap<>();
        if (subjectName != null && examName != null) {
            List<KimTaskSetting> kimSettings = kimTaskSettingRepository
                    .findBySubjectNameAndExamNameOrderByTaskNumberAsc(subjectName, examName);
            kimSettings.forEach(s -> kimMaxScoreByTaskNumber.put(s.getTaskNumber(), s.getMaxScore()));
        }

        List<com.example.test_platform.domain.entity.StudentAnswer> studentAnswers =
                studentAnswerRepository.findByAttemptIdOrderByTaskIdAsc(attempt.getId());
        Map<Long, com.example.test_platform.domain.entity.StudentAnswer> answerMap = studentAnswers.stream()
                .collect(Collectors.toMap(a -> a.getTask().getId(), a -> a, (e, r) -> r));

        int totalEarned = 0;
        int totalPossible = 0;

        for (GradeAttemptRequest.TaskGrade grade : req.getTaskGrades()) {
            if (grade.getTaskId() == null || grade.getScore() == null) continue;

            com.example.test_platform.domain.entity.StudentAnswer sa = answerMap.get(grade.getTaskId());
            if (sa == null) continue;

            int taskNum = sa.getTask().getTaskNumber() != null ? sa.getTask().getTaskNumber() : 0;
            int maxScore = kimMaxScoreByTaskNumber.getOrDefault(taskNum, 1);
            // Clamp: score cannot exceed KIM max or be negative
            int clamped = Math.max(0, Math.min(grade.getScore(), maxScore));
            sa.setManualScore(clamped);
            studentAnswerRepository.save(sa);

            totalEarned += clamped;
            totalPossible += maxScore;
        }

        // Recalculate overall score as percent
        int scorePercent = totalPossible > 0
                ? (int) Math.round((double) totalEarned / totalPossible * 100)
                : 0;
        attempt.setScorePercent(scorePercent);
        testAttemptRepository.save(attempt);
    }

    private TestVariant getTeacherVariantEntity(Long id, Long teacherId) {
        TestVariant variant = testVariantRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Test variant not found"));

        if (!variant.getTeacher().getId().equals(teacherId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied to this test variant");
        }

        return variant;
    }

    private List<Task> fetchTasksInOrder(List<Long> taskIds) {
        List<Task> tasks = new ArrayList<>();
        for (Long taskId : taskIds) {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Task with ID " + taskId + " not found"));
            tasks.add(task);
        }
        return tasks;
    }

    private void attachVariantTasks(TestVariant variant, List<Task> tasks) {
        for (int i = 0; i < tasks.size(); i++) {
            TestVariantTask variantTask = new TestVariantTask();
            variantTask.setTask(tasks.get(i));
            variantTask.setSortOrder(i);
            variant.addVariantTask(variantTask);
        }
    }

    private String generateUniqueAccessToken() {
        String token;
        do {
            token = UUID.randomUUID().toString().replace("-", "");
        } while (testVariantRepository.existsByAccessToken(token));
        return token;
    }

    private Double calculateAverageScore(List<TestAttempt> attempts) {
        List<TestAttempt> completed = attempts.stream()
                .filter(attempt -> attempt.getScorePercent() != null)
                .toList();

        if (completed.isEmpty()) {
            return null;
        }

        double sum = completed.stream()
                .mapToInt(TestAttempt::getScorePercent)
                .sum();
        return Math.round((sum / completed.size()) * 10.0) / 10.0;
    }
}
