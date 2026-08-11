package com.example.test_platform.controller;

import com.example.test_platform.domain.entity.User;
import com.example.test_platform.domain.enums.UserRole;
import com.example.test_platform.dto.request.ChangePasswordRequest;
import com.example.test_platform.dto.request.CreateUserRequest;
import com.example.test_platform.dto.request.LoginRequest;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
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
        teacher.setPasswordHash(passwordEncoder.encode("temp-pass"));
        teacher.setTemporaryPassword(true);
        teacher.setNextPaymentAt(LocalDateTime.now().plusDays(30));
        userRepository.save(teacher);
    }

    @Test
    void loginReturnsJwtToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setLogin("admin");
        request.setPassword("admin-pass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.login").value("admin"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void temporaryPasswordAllowsNavigationUntilChangedVoluntarily() throws Exception {
        String token = loginAndGetToken("teacher", "temp-pass");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").value(true));
    }


    @Test
    void adminCanCreateTeacherWithTemporaryPassword() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin-pass");

        CreateUserRequest request = new CreateUserRequest();
        request.setLogin("new-teacher");
        request.setDisplayName("New Teacher");
        request.setNextPaymentAt(LocalDateTime.now().plusMonths(1));

        MvcResult result = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty())
                .andExpect(jsonPath("$.user.login").value("new-teacher"))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String temporaryPassword = response.get("temporaryPassword").asText();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLogin("new-teacher");
        loginRequest.setPassword(temporaryPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.temporaryPassword").value(true));
    }

    @Test
    void adminCanUpdateTeacherSubscription() throws Exception {
        User teacher = userRepository.findByLogin("teacher").orElseThrow();
        String adminToken = loginAndGetToken("admin", "admin-pass");

        String body = """
                {
                  "subscriptionPaidAt": "2026-08-01T10:00:00",
                  "nextPaymentAt": "2026-09-01T10:00:00"
                }
                """;

        mockMvc.perform(put("/api/admin/users/" + teacher.getId() + "/subscription")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextPaymentAt").isNotEmpty());
    }

    @Test
    void changePasswordClearsTemporaryPasswordFlag() throws Exception {
        String token = loginAndGetToken("teacher", "temp-pass");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("temp-pass");
        request.setNewPassword("new-secure-password");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").value(false));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLogin("teacher");
        loginRequest.setPassword("new-secure-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.temporaryPassword").value(false));
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
        assertThat(response.get("token").asText()).isNotBlank();
        return response.get("token").asText();
    }
}
