package com.example.test_platform.repository;

import com.example.test_platform.domain.entity.Student;
import com.example.test_platform.domain.entity.Task;
import com.example.test_platform.domain.entity.TestVariant;
import com.example.test_platform.domain.entity.User;
import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import com.example.test_platform.domain.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestVariantRepository testVariantRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void repositoriesPersistAndFindCoreEntities() {
        User teacher = new User();
        teacher.setLogin("teacher1");
        teacher.setPasswordHash("hash");
        teacher.setDisplayName("Иван Петров");
        teacher.setRole(UserRole.TEACHER);
        teacher = userRepository.save(teacher);

        Task task = new Task();
        task.setPublicId("TASK-001");
        task.setSubject(Subject.MATHEMATICS);
        task.setTaskBank(TaskBank.FIPI);
        task.setExamType(ExamType.EGE);
        task.setTaskNumber(1);
        task.setSubtopic("Алгебра");
        task.setContent("Найдите значение выражения $2+2$");
        task.setCorrectAnswer("4");
        taskRepository.save(task);

        TestVariant variant = new TestVariant();
        variant.setTeacher(teacher);
        variant.setTitle("Вариант 1");
        variant.setAccessToken("token-abc");
        variant.setExamType(ExamType.EGE);
        variant.setSubject(Subject.MATHEMATICS);
        variant.setTaskBank(TaskBank.FIPI);
        testVariantRepository.save(variant);

        Student student = new Student();
        student.setTeacher(teacher);
        student.setDisplayName("Алексей");
        student.setBrowserFingerprint("fp-123");
        studentRepository.save(student);

        assertThat(userRepository.findByLogin("teacher1")).isPresent();
        assertThat(taskRepository.findByPublicId("TASK-001")).isPresent();
        assertThat(testVariantRepository.findByAccessToken("token-abc")).isPresent();
        assertThat(studentRepository.findByTeacherIdAndDisplayNameIgnoreCaseAndBrowserFingerprint(
                teacher.getId(), "алексей", "fp-123"
        )).isPresent();
        assertThat(userRepository.findByRoleOrderByDisplayNameAsc(UserRole.TEACHER))
                .extracting(User::getLogin)
                .containsExactly("teacher1");
    }

    @Test
    void userRepositoryFindsTeachersForAdminPanel() {
        User admin = new User();
        admin.setLogin("admin");
        admin.setPasswordHash("hash");
        admin.setDisplayName("Администратор");
        admin.setRole(UserRole.ADMIN);
        admin.setLastLoginAt(LocalDateTime.now());
        userRepository.save(admin);

        assertThat(userRepository.findByRoleOrderByDisplayNameAsc(UserRole.ADMIN))
                .hasSize(1)
                .first()
                .extracting(User::getLogin)
                .isEqualTo("admin");
    }
}
