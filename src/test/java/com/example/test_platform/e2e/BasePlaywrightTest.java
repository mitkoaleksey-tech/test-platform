package com.example.test_platform.e2e;

import com.example.test_platform.domain.entity.Task;
import com.example.test_platform.domain.entity.User;
import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import com.example.test_platform.domain.enums.UserRole;
import com.example.test_platform.repository.TaskRepository;
import com.example.test_platform.repository.UserRepository;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BasePlaywrightTest {

    @LocalServerPort
    protected int port;

    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected TaskRepository taskRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeAll
    static void launchBrowser() {
        boolean headless = Boolean.parseBoolean(System.getProperty("playwright.headless", "true"));
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void createContextAndPage() {
        // Ensure default admin exists
        if (userRepository.findByLogin("admin").isEmpty()) {
            User admin = new User();
            admin.setLogin("admin");
            admin.setDisplayName("Администратор");
            admin.setRole(UserRole.ADMIN);
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setTemporaryPassword(false);
            userRepository.save(admin);
        }

        context = browser.newContext(new Browser.NewContextOptions()
                .setPermissions(java.util.List.of("clipboard-read", "clipboard-write")));
        page = context.newPage();
        page.onDialog(dialog -> dialog.accept());
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Create a teacher user programmatically with a known password.
     */
    protected User createTeacher(String login, String displayName, String password) {
        return userRepository.findByLogin(login).orElseGet(() -> {
            User teacher = new User();
            teacher.setLogin(login);
            teacher.setDisplayName(displayName);
            teacher.setRole(UserRole.TEACHER);
            teacher.setPasswordHash(passwordEncoder.encode(password));
            teacher.setTemporaryPassword(false);
            return userRepository.save(teacher);
        });
    }

    /**
     * Create a task programmatically.
     */
    protected Task createTask(String subtopic, String content, String correctAnswer) {
        Task task = new Task();
        task.setPublicId(UUID.randomUUID().toString().substring(0, 8));
        task.setSubject(Subject.MATHEMATICS);
        task.setExamType(ExamType.EGE);
        task.setTaskBank(TaskBank.FIPI);
        task.setTaskNumber(1);
        task.setSubtopic(subtopic);
        task.setContent(content);
        task.setCorrectAnswer(correctAnswer);
        return taskRepository.save(task);
    }

    /**
     * Log in via UI as a given user.
     */
    protected void loginViaUi(String login, String password) {
        page.navigate(baseUrl() + "/#login");
        page.waitForSelector("#login-form");
        page.fill("#login-input", login);
        page.fill("#password-input", password);
        page.click("button[type='submit']");
        page.waitForSelector(".navbar-brand");
    }
}
