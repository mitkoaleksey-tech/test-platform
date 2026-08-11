package com.example.test_platform.controller;

import com.example.test_platform.domain.entity.Task;
import com.example.test_platform.domain.entity.User;
import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import com.example.test_platform.domain.enums.UserRole;
import com.example.test_platform.dto.request.CreateTestVariantRequest;
import com.example.test_platform.dto.request.LoginRequest;
import com.example.test_platform.dto.request.UpdateTestVariantRequest;
import com.example.test_platform.repository.TaskRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.test_platform.repository.StudentAnswerRepository;
import com.example.test_platform.repository.StudentRepository;
import com.example.test_platform.repository.TestAttemptRepository;
import com.example.test_platform.repository.TaskRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherTestVariantControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private StudentAnswerRepository studentAnswerRepository;

    @Autowired
    private TestAttemptRepository testAttemptRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TestVariantRepository testVariantRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String teacher1Token;
    private String teacher2Token;

    @BeforeEach
    void setUp() throws Exception {
        studentAnswerRepository.deleteAll();
        testAttemptRepository.deleteAll();
        studentRepository.deleteAll();
        testVariantRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();

        User teacher1 = new User();
        teacher1.setLogin("teacher1");
        teacher1.setDisplayName("Teacher One");
        teacher1.setRole(UserRole.TEACHER);
        teacher1.setPasswordHash(passwordEncoder.encode("pass1"));
        teacher1.setTemporaryPassword(false);
        userRepository.save(teacher1);

        User teacher2 = new User();
        teacher2.setLogin("teacher2");
        teacher2.setDisplayName("Teacher Two");
        teacher2.setRole(UserRole.TEACHER);
        teacher2.setPasswordHash(passwordEncoder.encode("pass2"));
        teacher2.setTemporaryPassword(false);
        userRepository.save(teacher2);

        teacher1Token = loginAndGetToken("teacher1", "pass1");
        teacher2Token = loginAndGetToken("teacher2", "pass2");
    }

    @Test
    void createVariant_AsTeacher_CreatesVariantWithTasksAndAccessToken() throws Exception {
        Task task1 = createTestTask(1, "Subtopic 1");
        Task task2 = createTestTask(2, "Subtopic 2");

        CreateTestVariantRequest request = new CreateTestVariantRequest();
        request.setTitle("Вариант 1 по математике");
        request.setSubject(Subject.MATHEMATICS);
        request.setExamType(ExamType.EGE);
        request.setTaskBank(TaskBank.FIPI);
        request.setTaskIds(List.of(task1.getId(), task2.getId()));

        mockMvc.perform(post("/api/teacher/variants")
                        .header("Authorization", "Bearer " + teacher1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Вариант 1 по математике"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.shareableUrl").value(org.hamcrest.Matchers.startsWith("/test/")))
                .andExpect(jsonPath("$.tasks.length()").value(2))
                .andExpect(jsonPath("$.totalTasks").value(2));

        assertThat(testVariantRepository.count()).isEqualTo(1);
    }

    @Test
    void getMyVariants_AsTeacher_ReturnsOnlyTeacherVariants() throws Exception {
        Task task = createTestTask(1, "Subtopic");
        createVariantForTeacher("teacher1", "Вариант Учителя 1", List.of(task.getId()));
        createVariantForTeacher("teacher2", "Вариант Учителя 2", List.of(task.getId()));

        mockMvc.perform(get("/api/teacher/variants")
                        .header("Authorization", "Bearer " + teacher1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Вариант Учителя 1"));
    }

    @Test
    void updateVariant_AsTeacher_UpdatesTitleAndTasks() throws Exception {
        Task task1 = createTestTask(1, "Subtopic 1");
        Task task2 = createTestTask(2, "Subtopic 2");

        MvcResult createResult = createVariantForTeacher("teacher1", "Исходный вариант", List.of(task1.getId()));
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long variantId = created.get("id").asLong();

        UpdateTestVariantRequest updateRequest = new UpdateTestVariantRequest();
        updateRequest.setTitle("Обновленный вариант");
        updateRequest.setTaskIds(List.of(task2.getId(), task1.getId()));

        mockMvc.perform(put("/api/teacher/variants/" + variantId)
                        .header("Authorization", "Bearer " + teacher1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Обновленный вариант"))
                .andExpect(jsonPath("$.tasks[0].id").value(task2.getId()))
                .andExpect(jsonPath("$.tasks[1].id").value(task1.getId()));
    }

    @Test
    void deleteVariant_AsTeacher_DeletesVariant() throws Exception {
        Task task = createTestTask(1, "Subtopic");
        MvcResult createResult = createVariantForTeacher("teacher1", "На удаление", List.of(task.getId()));
        Long variantId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/teacher/variants/" + variantId)
                        .header("Authorization", "Bearer " + teacher1Token))
                .andExpect(status().isNoContent());

        assertThat(testVariantRepository.existsById(variantId)).isFalse();
    }

    @Test
    void downloadPdf_AsTeacher_ReturnsPdfDocument() throws Exception {
        Task task = createTestTask(1, "Геометрия");
        MvcResult createResult = createVariantForTeacher("teacher1", "Печатный вариант", List.of(task.getId()));
        Long variantId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/teacher/variants/" + variantId + "/pdf")
                        .header("Authorization", "Bearer " + teacher1Token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"variant-" + variantId + ".pdf\""));
    }

    @Test
    void teacherCannotAccessOtherTeacherVariant() throws Exception {
        Task task = createTestTask(1, "Subtopic");
        MvcResult createResult = createVariantForTeacher("teacher1", "Чужой вариант", List.of(task.getId()));
        Long variantId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/teacher/variants/" + variantId)
                        .header("Authorization", "Bearer " + teacher2Token))
                .andExpect(status().isForbidden());
    }

    private Task createTestTask(int number, String subtopic) {
        Task task = new Task();
        task.setPublicId("T-TEST" + System.nanoTime());
        task.setSubject(Subject.MATHEMATICS);
        task.setExamType(ExamType.EGE);
        task.setTaskBank(TaskBank.FIPI);
        task.setTaskNumber(number);
        task.setSubtopic(subtopic);
        task.setContent("Content for task " + number);
        task.setCorrectAnswer("10");
        task.setActive(true);
        return taskRepository.save(task);
    }

    private MvcResult createVariantForTeacher(String login, String title, List<Long> taskIds) throws Exception {
        String token = loginAndGetToken(login, login.equals("teacher1") ? "pass1" : "pass2");

        CreateTestVariantRequest request = new CreateTestVariantRequest();
        request.setTitle(title);
        request.setSubject(Subject.MATHEMATICS);
        request.setExamType(ExamType.EGE);
        request.setTaskBank(TaskBank.FIPI);
        request.setTaskIds(taskIds);

        return mockMvc.perform(post("/api/teacher/variants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String loginAndGetToken(String login, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setLogin(login);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("token").asText();
    }
}
