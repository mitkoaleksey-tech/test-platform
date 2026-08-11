package com.example.test_platform.controller;

import com.example.test_platform.domain.entity.Task;
import com.example.test_platform.domain.entity.TestVariant;
import com.example.test_platform.domain.entity.TestVariantTask;
import com.example.test_platform.domain.entity.User;
import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import com.example.test_platform.domain.enums.UserRole;
import com.example.test_platform.dto.request.StartTestAttemptRequest;
import com.example.test_platform.dto.request.StudentAnswerDto;
import com.example.test_platform.dto.request.SubmitTestAttemptRequest;
import com.example.test_platform.repository.StudentRepository;
import com.example.test_platform.repository.TaskRepository;
import com.example.test_platform.repository.TestAttemptRepository;
import com.example.test_platform.repository.TestVariantRepository;
import com.example.test_platform.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicStudentTestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private TestVariantRepository testVariantRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TestAttemptRepository testAttemptRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User teacher;
    private TestVariant variant;
    private Task gradableTask1;
    private Task gradableTask2;
    private Task ungradableTask;

    @BeforeEach
    void setUp() {
        testAttemptRepository.deleteAll();
        studentRepository.deleteAll();
        testVariantRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();

        teacher = new User();
        teacher.setLogin("teacher");
        teacher.setDisplayName("Teacher");
        teacher.setRole(UserRole.TEACHER);
        teacher.setPasswordHash(passwordEncoder.encode("pass"));
        teacher.setTemporaryPassword(false);
        userRepository.save(teacher);

        gradableTask1 = createTestTask(1, "Subtopic 1", "3.5");
        gradableTask2 = createTestTask(2, "Subtopic 2", "42");
        ungradableTask = createTestTask(3, "Subtopic 3", null);

        variant = new TestVariant();
        variant.setTeacher(teacher);
        variant.setTitle("Тестовый вариант");
        variant.setSubject(Subject.MATHEMATICS);
        variant.setExamType(ExamType.EGE);
        variant.setTaskBank(TaskBank.FIPI);
        variant.setAccessToken(UUID.randomUUID().toString().replace("-", ""));

        addVariantTask(variant, gradableTask1, 0);
        addVariantTask(variant, gradableTask2, 1);
        addVariantTask(variant, ungradableTask, 2);

        variant = testVariantRepository.save(variant);
    }

    @Test
    void getPublicTestVariant_ReturnsTasksWithoutCorrectAnswers() throws Exception {
        mockMvc.perform(get("/api/public/tests/" + variant.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Тестовый вариант"))
                .andExpect(jsonPath("$.tasks.length()").value(3))
                .andExpect(jsonPath("$.tasks[0].correctAnswer").doesNotExist());
    }

    @Test
    void startAndSubmitTestAttempt_GradesOnlyTasksWithAnswers() throws Exception {
        StartTestAttemptRequest startRequest = new StartTestAttemptRequest();
        startRequest.setStudentName("Иван Иванов");
        startRequest.setBrowserFingerprint("fp-123");

        MvcResult startResult = mockMvc.perform(post("/api/public/tests/" + variant.getAccessToken() + "/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").isNotEmpty())
                .andExpect(jsonPath("$.studentName").value("Иван Иванов"))
                .andReturn();

        JsonNode startResponse = objectMapper.readTree(startResult.getResponse().getContentAsString());
        Long attemptId = startResponse.get("attemptId").asLong();

        StudentAnswerDto ans1 = new StudentAnswerDto();
        ans1.setTaskId(gradableTask1.getId());
        ans1.setAnswer("3,5");

        StudentAnswerDto ans2 = new StudentAnswerDto();
        ans2.setTaskId(gradableTask2.getId());
        ans2.setAnswer("100");

        StudentAnswerDto ans3 = new StudentAnswerDto();
        ans3.setTaskId(ungradableTask.getId());
        ans3.setAnswer("Произвольное решение");

        SubmitTestAttemptRequest submitRequest = new SubmitTestAttemptRequest();
        submitRequest.setAttemptId(attemptId);
        submitRequest.setAnswers(List.of(ans1, ans2, ans3));

        mockMvc.perform(post("/api/public/tests/" + variant.getAccessToken() + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(3))
                .andExpect(jsonPath("$.gradableTasks").value(2))
                .andExpect(jsonPath("$.correctCount").value(1))
                .andExpect(jsonPath("$.scorePercent").value(50))
                .andExpect(jsonPath("$.feedback[0].status").value("CORRECT"))
                .andExpect(jsonPath("$.feedback[1].status").value("INCORRECT"))
                .andExpect(jsonPath("$.feedback[2].status").value("UNGRADED"));

        assertThat(studentRepository.count()).isEqualTo(1);
    }

    private Task createTestTask(int number, String subtopic, String correctAnswer) {
        Task task = new Task();
        task.setPublicId("T-TEST" + System.nanoTime());
        task.setSubject(Subject.MATHEMATICS);
        task.setExamType(ExamType.EGE);
        task.setTaskBank(TaskBank.FIPI);
        task.setTaskNumber(number);
        task.setSubtopic(subtopic);
        task.setContent("Content " + number);
        task.setCorrectAnswer(correctAnswer);
        task.setActive(true);
        return taskRepository.save(task);
    }

    private void addVariantTask(TestVariant variant, Task task, int sortOrder) {
        TestVariantTask vt = new TestVariantTask();
        vt.setTask(task);
        vt.setSortOrder(sortOrder);
        variant.addVariantTask(vt);
    }
}
