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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentDetailedAnswerIntegrationTest {

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
    private com.example.test_platform.repository.StudentAnswerAttachmentRepository studentAnswerAttachmentRepository;

    @Autowired
    private com.example.test_platform.repository.StudentAnswerRepository studentAnswerRepository;

    @Autowired
    private com.example.test_platform.repository.TestVariantTaskRepository testVariantTaskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User teacher;
    private TestVariant variant;
    private Task detailedTask;

    @BeforeEach
    void setUp() {
        studentAnswerAttachmentRepository.deleteAll();
        studentAnswerRepository.deleteAll();
        testAttemptRepository.deleteAll();
        studentRepository.deleteAll();
        testVariantTaskRepository.deleteAll();
        testVariantRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();

        teacher = new User();
        teacher.setLogin("teacher_det");
        teacher.setDisplayName("Teacher Detailed");
        teacher.setRole(UserRole.TEACHER);
        teacher.setPasswordHash(passwordEncoder.encode("pass"));
        teacher.setTemporaryPassword(false);
        userRepository.save(teacher);

        detailedTask = new Task();
        detailedTask.setPublicId("T-DET" + System.nanoTime());
        detailedTask.setSubject(Subject.MATHEMATICS);
        detailedTask.setExamType(ExamType.EGE);
        detailedTask.setTaskBank(TaskBank.FIPI);
        detailedTask.setTaskNumber(12);
        detailedTask.setSubtopic("Тригонометрия развёрнутая");
        detailedTask.setContent("Решите уравнение: \\sin x = 1/2");
        detailedTask.setCorrectAnswer(null);
        detailedTask.setHasDetailedAnswer(true);
        detailedTask.setActive(true);
        detailedTask = taskRepository.save(detailedTask);

        variant = new TestVariant();
        variant.setTeacher(teacher);
        variant.setTitle("Вариант с развёрнутым ответом");
        variant.setSubject(Subject.MATHEMATICS);
        variant.setExamType(ExamType.EGE);
        variant.setTaskBank(TaskBank.FIPI);
        variant.setAccessToken(UUID.randomUUID().toString().replace("-", ""));

        TestVariantTask vt = new TestVariantTask();
        vt.setTask(detailedTask);
        vt.setSortOrder(0);
        variant.addVariantTask(vt);

        variant = testVariantRepository.save(variant);
    }

    @Test
    void testDetailedAnswerFlow_WithAttachmentUpload() throws Exception {
        // 1. Check public task response contains hasDetailedAnswer = true
        mockMvc.perform(get("/api/public/tests/" + variant.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks[0].hasDetailedAnswer").value(true));

        // 2. Start attempt
        StartTestAttemptRequest startRequest = new StartTestAttemptRequest();
        startRequest.setStudentName("Петров Пётр");

        MvcResult startResult = mockMvc.perform(post("/api/public/tests/" + variant.getAccessToken() + "/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode startResp = objectMapper.readTree(startResult.getResponse().getContentAsString());
        Long attemptId = startResp.get("attemptId").asLong();

        // 3. Upload photo/file attachment
        MockMultipartFile file = new MockMultipartFile("file", "solution.png", "image/png", new byte[]{1, 2, 3, 4});

        mockMvc.perform(multipart("/api/public/tests/" + variant.getAccessToken() + "/attempts/" + attemptId + "/tasks/" + detailedTask.getId() + "/attachments")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalFilename").value("solution.png"))
                .andExpect(jsonPath("$.isImage").value(true));

        // 4. Submit attempt with multiline answer
        StudentAnswerDto ans = new StudentAnswerDto();
        ans.setTaskId(detailedTask.getId());
        ans.setAnswer("a) x = \\pi/6 + 2\\pi k\nб) x = 5\\pi/6");

        SubmitTestAttemptRequest submitReq = new SubmitTestAttemptRequest();
        submitReq.setAttemptId(attemptId);
        submitReq.setAnswers(List.of(ans));

        mockMvc.perform(post("/api/public/tests/" + variant.getAccessToken() + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedback[0].status").value("UNGRADED"));
    }
}
