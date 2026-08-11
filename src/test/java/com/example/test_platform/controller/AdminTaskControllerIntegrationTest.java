package com.example.test_platform.controller;

import com.example.test_platform.config.StorageProperties;
import com.example.test_platform.domain.entity.Task;
import com.example.test_platform.domain.entity.User;
import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import com.example.test_platform.domain.enums.UserRole;
import com.example.test_platform.dto.request.CreateTaskRequest;
import com.example.test_platform.dto.request.LoginRequest;
import com.example.test_platform.dto.request.UpdateTaskRequest;
import com.example.test_platform.repository.TaskImageRepository;
import com.example.test_platform.repository.TaskRepository;
import com.example.test_platform.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.test_platform.repository.StudentAnswerRepository;
import com.example.test_platform.repository.StudentRepository;
import com.example.test_platform.repository.TestAttemptRepository;
import com.example.test_platform.repository.TestVariantRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

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
    private TaskImageRepository taskImageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StorageProperties storageProperties;

    private String adminToken;
    private String teacherToken;

    @BeforeEach
    void setUp() throws Exception {
        cleanStorage();
        studentAnswerRepository.deleteAll();
        testAttemptRepository.deleteAll();
        studentRepository.deleteAll();
        testVariantRepository.deleteAll();
        taskImageRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();

        User admin = new User();
        admin.setLogin("admin");
        admin.setDisplayName("Admin");
        admin.setRole(UserRole.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode("admin-pass"));
        admin.setTemporaryPassword(false);
        userRepository.save(admin);

        User teacher = new User();
        teacher.setLogin("teacher");
        teacher.setDisplayName("Teacher");
        teacher.setRole(UserRole.TEACHER);
        teacher.setPasswordHash(passwordEncoder.encode("teacher-pass"));
        teacher.setTemporaryPassword(false);
        userRepository.save(teacher);

        adminToken = loginAndGetToken("admin", "admin-pass");
        teacherToken = loginAndGetToken("teacher", "teacher-pass");
    }

    @AfterEach
    void tearDown() {
        cleanStorage();
    }

    @Test
    void createTask_AsAdmin_CreatesTaskAndGeneratesPublicId() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setSubject(Subject.MATHEMATICS);
        request.setExamType(ExamType.EGE);
        request.setTaskBank(TaskBank.FIPI);
        request.setTaskNumber(12);
        request.setSubtopic("Производная функции");
        request.setContent("Найдите наименьшее значение функции $y = x^3 - 3x + 5$");
        request.setCorrectAnswer("3");

        mockMvc.perform(post("/api/admin/tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.publicId").value(org.hamcrest.Matchers.startsWith("T-")))
                .andExpect(jsonPath("$.subject").value("MATHEMATICS"))
                .andExpect(jsonPath("$.taskNumber").value(12))
                .andExpect(jsonPath("$.subtopic").value("Производная функции"))
                .andExpect(jsonPath("$.active").value(true));

        assertThat(taskRepository.count()).isEqualTo(1);
    }

    @Test
    void getTasks_AsAdmin_SupportsFiltering() throws Exception {
        createTestTask(Subject.MATHEMATICS, ExamType.EGE, TaskBank.FIPI, 1, true);
        createTestTask(Subject.PHYSICS, ExamType.EGE, TaskBank.FIPI, 2, true);
        createTestTask(Subject.MATHEMATICS, ExamType.OGE, TaskBank.STATGRAD, 1, false);

        mockMvc.perform(get("/api/admin/tasks?subject=MATHEMATICS")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/admin/tasks?subject=MATHEMATICS&active=true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].taskNumber").value(1));
    }

    @Test
    void updateTask_AsAdmin_UpdatesTaskDetails() throws Exception {
        Task task = createTestTask(Subject.MATHEMATICS, ExamType.EGE, TaskBank.FIPI, 5, true);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setSubject(Subject.MATHEMATICS);
        request.setExamType(ExamType.OGE);
        request.setTaskBank(TaskBank.STATGRAD);
        request.setTaskNumber(6);
        request.setSubtopic("Геометрия");
        request.setContent("Обновленный текст задачи");
        request.setCorrectAnswer("42");
        request.setActive(false);

        mockMvc.perform(put("/api/admin/tasks/" + task.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examType").value("OGE"))
                .andExpect(jsonPath("$.taskNumber").value(6))
                .andExpect(jsonPath("$.active").value(false));

        Task updated = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(updated.getExamType()).isEqualTo(ExamType.OGE);
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void deleteTask_AsAdmin_DeletesTask() throws Exception {
        Task task = createTestTask(Subject.MATHEMATICS, ExamType.EGE, TaskBank.FIPI, 1, true);

        mockMvc.perform(delete("/api/admin/tasks/" + task.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(taskRepository.existsById(task.getId())).isFalse();
    }

    @Test
    void uploadAndServeAndDeleteImage_FullCycle() throws Exception {
        Task task = createTestTask(Subject.MATHEMATICS, ExamType.EGE, TaskBank.FIPI, 1, true);

        byte[] imageBytes = createSampleImageBytes(1200, 800, "png");
        MockMultipartFile file = new MockMultipartFile("file", "diagram.png", "image/png", imageBytes);

        MvcResult uploadResult = mockMvc.perform(multipart("/api/admin/tasks/" + task.getId() + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.widthPx").value(storageProperties.getImageTargetWidth()))
                .andExpect(jsonPath("$.originalFilename").value("diagram.png"))
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.containsString("/api/public/images/")))
                .andReturn();

        JsonNode imageResponse = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        Long imageId = imageResponse.get("id").asLong();
        String imageUrl = imageResponse.get("url").asText();

        String relativePath = imageUrl.substring(imageUrl.indexOf("/api/public/images/") + "/api/public/images/".length());

        mockMvc.perform(get("/api/public/images/" + relativePath))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));

        mockMvc.perform(delete("/api/admin/tasks/" + task.getId() + "/images/" + imageId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(taskImageRepository.existsById(imageId)).isFalse();

        mockMvc.perform(get("/api/public/images/" + relativePath))
                .andExpect(status().isNotFound());
    }

    @Test
    void teacherCannotAccessAdminTaskMutationEndpoints() throws Exception {
        mockMvc.perform(post("/api/admin/tasks")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private Task createTestTask(Subject subject, ExamType examType, TaskBank bank, int number, boolean active) {
        Task task = new Task();
        task.setPublicId("T-TEST" + System.nanoTime());
        task.setSubject(subject);
        task.setExamType(examType);
        task.setTaskBank(bank);
        task.setTaskNumber(number);
        task.setSubtopic("Test subtopic");
        task.setContent("Test content");
        task.setCorrectAnswer("10");
        task.setActive(active);
        return taskRepository.save(task);
    }

    private byte[] createSampleImageBytes(int width, int height, String format) throws Exception {
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, format, outputStream);
        return outputStream.toByteArray();
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

    private void cleanStorage() {
        try {
            Path path = Path.of(storageProperties.getImagesPath());
            if (Files.exists(path)) {
                try (var stream = Files.walk(path)) {
                    stream.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
