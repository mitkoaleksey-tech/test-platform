package com.example.test_platform.controller;

import com.example.test_platform.domain.entity.Student;
import com.example.test_platform.domain.entity.User;
import com.example.test_platform.domain.enums.UserRole;
import com.example.test_platform.repository.StudentRepository;
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
class TeacherStudentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.example.test_platform.repository.StudentRepository studentRepository;

    @Autowired
    private com.example.test_platform.repository.StudentAnswerRepository studentAnswerRepository;


    @Autowired
    private com.example.test_platform.repository.TestAttemptRepository testAttemptRepository;

    @Autowired
    private com.example.test_platform.repository.TestVariantRepository testVariantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String teacherToken;

    @BeforeEach
    void setUp() {
        studentAnswerRepository.deleteAll();
        testAttemptRepository.deleteAll();
        testVariantRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();


        User teacher = new User();
        teacher.setLogin("teacher1");
        teacher.setDisplayName("Учитель Физики");
        teacher.setRole(UserRole.TEACHER);
        teacher.setPasswordHash(passwordEncoder.encode("password"));
        User savedTeacher = userRepository.save(teacher);

        Student student = new Student();
        student.setTeacher(savedTeacher);
        student.setDisplayName("Петров Сергей");
        student.setBrowserFingerprint("fp-browser-xyz");
        studentRepository.save(student);

        teacherToken = jwtService.generateToken(new UserPrincipal(savedTeacher));

    }

    @Test
    void getTeacherStudents_ReturnsStudentsList() throws Exception {
        mockMvc.perform(get("/api/teacher/students")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].displayName").value("Петров Сергей"))
                .andExpect(jsonPath("$[0].browserFingerprint").value("fp-browser-xyz"));
    }
}
