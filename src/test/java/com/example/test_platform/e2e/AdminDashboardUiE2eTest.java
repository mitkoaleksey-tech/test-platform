package com.example.test_platform.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class AdminDashboardUiE2eTest extends BasePlaywrightTest {

    @BeforeEach
    void loginAsAdmin() {
        loginViaUi("admin", "admin123");
    }

    @Test
    void testSwitchAdminTabs() {
        // Default tab is Teachers
        page.waitForSelector("h2:has-text('Управление Преподавателями')");
        assertThat(page.locator("h2:has-text('Управление Преподавателями')")).isVisible();

        // Switch to Task Bank tab
        page.click("button:has-text('Банк Задач')");
        page.waitForSelector("h2:has-text('Банк Задач')");
        assertThat(page.locator("h2:has-text('Банк Задач')")).isVisible();

        // Switch to Dictionaries tab
        page.click("button:has-text('Справочники')");
        page.waitForSelector("h3:has-text('Предметы')");
        assertThat(page.locator("h3:has-text('Предметы')")).isVisible();

        // Switch to Monitoring tab
        page.click("button:has-text('Мониторинг')");
        page.waitForSelector(".metric-card");
        assertThat(page.locator(".metric-card").first()).isVisible();
    }

    @Test
    void testCreateTeacherFlow() {
        page.waitForSelector("button:has-text('+ Добавить преподавателя')");
        page.click("button:has-text('+ Добавить преподавателя')");

        page.waitForSelector("#create-teacher-form");
        page.fill("#teacher-name", "Петрова Елена");
        page.fill("#teacher-login", "teacher_petrova");

        page.click("#create-teacher-form button[type='submit']");

        // Wait for table to refresh with new teacher data
        page.waitForSelector("tbody");
        assertThat(page.locator("tbody")).containsText("Петрова Елена");
        assertThat(page.locator("tbody")).containsText("teacher_petrova");
    }

    @Test
    void testCreateTaskWithFipiVectorNormalization() {
        page.click("button:has-text('Банк Задач')");
        page.waitForSelector("button:has-text('+ Создать задачу')");
        page.click("button:has-text('+ Создать задачу')");

        page.waitForSelector("#create-task-form");
        page.fill("#task-subtopic", "Векторный анализ");
        page.fill("#task-content", "Даны векторы a(25;0) и b(1;-5).");
        page.fill("#task-correct-answer", "20");

        page.click("#create-task-form button[type='submit']");

        // Wait for task table to refresh
        page.waitForSelector("tbody");
        assertThat(page.locator("tbody")).containsText("Векторный анализ");
    }

    @Test
    void testDynamicDictionaries() {
        page.click("button:has-text('Справочники')");
        page.waitForSelector("#dict-subjects-list");

        assertThat(page.locator("h3:has-text('Предметы')")).isVisible();
        assertThat(page.locator("h3:has-text('Типы Экзаменов')")).isVisible();
        assertThat(page.locator("h3:has-text('Банки Задач')")).isVisible();
    }

    @Test
    void testServerMonitoring() {
        page.click("button:has-text('Мониторинг')");
        page.waitForSelector(".metrics-grid");

        assertThat(page.locator(".metrics-grid")).isVisible();
        assertThat(page.locator(".metric-card")).hasCount(4);
    }
}
