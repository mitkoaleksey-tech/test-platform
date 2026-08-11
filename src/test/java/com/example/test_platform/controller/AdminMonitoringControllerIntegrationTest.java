package com.example.test_platform.controller;

import com.example.test_platform.domain.entity.User;
import com.example.test_platform.domain.enums.UserRole;
import com.example.test_platform.repository.UserRepository;
import com.example.test_platform.security.JwtService;
import com.example.test_platform.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminMonitoringControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.example.test_platform.repository.StudentAnswerRepository studentAnswerRepository;

    @Autowired
    private com.example.test_platform.repository.TestAttemptRepository testAttemptRepository;

    @Autowired
    private com.example.test_platform.repository.TestVariantRepository testVariantRepository;

    @Autowired
    private com.example.test_platform.repository.StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;

    @BeforeEach
    void setUp() {
        studentAnswerRepository.deleteAll();
        testAttemptRepository.deleteAll();
        testVariantRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();


        User admin = new User();
        admin.setLogin("admin");
        admin.setDisplayName("Администратор");
        admin.setRole(UserRole.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        User saved = userRepository.save(admin);

        adminToken = jwtService.generateToken(new UserPrincipal(saved));

    }

    @Test
    void getMetrics_AsAdmin_ReturnsServerMetrics() throws Exception {
        mockMvc.perform(get("/api/admin/monitoring")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpuUsagePercent").exists())
                .andExpect(jsonPath("$.totalMemoryMb").exists())
                .andExpect(jsonPath("$.usedMemoryMb").exists())
                .andExpect(jsonPath("$.dockerStatus").exists())
                .andExpect(jsonPath("$.appStatus").value("HEALTHY"));
    }
}
