package com.example.test_platform.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class StudentTestUiE2eTest extends BasePlaywrightTest {

    private static final String TEACHER_LOGIN = "teacher_student_e2e";
    private static final String TEACHER_PASS = "teacherPass456";

    @BeforeEach
    void setupTeacherAndTask() {
        // Create teacher and task programmatically
        createTeacher(TEACHER_LOGIN, "Преподаватель Тестов", TEACHER_PASS);
        createTask("Корни и степени", "Найдите корень уравнения.", "-6");
    }

    private String createVariantAndExtractToken() {
        loginViaUi(TEACHER_LOGIN, TEACHER_PASS);

        page.click("button:has-text('Собрать новый вариант')");
        page.waitForSelector("button:has-text('Сгенерировать вариант')");
        page.click("button:has-text('+')");
        page.click("button:has-text('Сгенерировать вариант')");

        page.waitForSelector(".variant-grid");
        String pageHtml = page.content();
        int tokenIdx = pageHtml.indexOf("#test/");
        if (tokenIdx < 0) {
            throw new AssertionError("Could not find test token link in page");
        }
        String afterToken = pageHtml.substring(tokenIdx + 6);
        int endIdx = afterToken.indexOf("'");
        if (endIdx < 0) endIdx = afterToken.indexOf("\"");
        return (endIdx > 0) ? afterToken.substring(0, endIdx) : afterToken.substring(0, 36);
    }

    private void openTestLinkFresh(String testToken) {
        page.navigate(baseUrl() + "/#test/" + testToken);
        page.evaluate("() => { if (typeof state !== 'undefined') { state.studentResult = null; state.studentAttempt = null; } }");
        page.reload();
        page.waitForSelector("#student-name-input");
    }

    @Test
    void testStudentFlowSingleAttempt() {
        String testToken = createVariantAndExtractToken();

        // Student opens test link
        openTestLinkFresh(testToken);

        // Fill student name (Cyrillic) and start test
        page.fill("#student-name-input", "Сидоров Алексей");
        page.click("button[type='submit']");

        // Solve task
        page.waitForSelector(".task-card");
        page.fill("input[placeholder='Введите ответ']", "-6");
        page.click("button:has-text('Завершить и сдать тест')");

        // Verify result screen
        page.waitForSelector(".score-hero");
        assertThat(page.locator(".score-hero")).containsText("Тест завершен");
    }

    @Test
    void testBlockReattempt() {
        String testToken = createVariantAndExtractToken();

        // First attempt
        openTestLinkFresh(testToken);
        page.fill("#student-name-input", "Иванова Мария");
        page.click("button[type='submit']");

        page.waitForSelector(".task-card");
        page.fill("input[placeholder='Введите ответ']", "-6");
        page.click("button:has-text('Завершить и сдать тест')");
        page.waitForSelector(".score-hero");

        // Second attempt with same student name
        openTestLinkFresh(testToken);
        page.fill("#student-name-input", "Иванова Мария");
        page.click("button[type='submit']");

        // Verify reattempt block error message
        page.waitForSelector("#st-error");
        assertThat(page.locator("#st-error")).isVisible();
        assertThat(page.locator("#st-error")).containsText("Вы уже сдали этот тест");
    }

    @Test
    void testAllowRetakeByTeacherAndHistory() {
        String testToken = createVariantAndExtractToken();

        // Student completes test first time
        openTestLinkFresh(testToken);
        page.fill("#student-name-input", "Ковалев Дмитрий");
        page.click("button[type='submit']");

        page.waitForSelector(".task-card");
        page.fill("input[placeholder='Введите ответ']", "-6");
        page.click("button:has-text('Завершить и сдать тест')");
        page.waitForSelector(".score-hero");

        // Teacher opens test results modal and allows retake
        loginViaUi(TEACHER_LOGIN, TEACHER_PASS);
        page.waitForSelector(".variant-grid button:has-text('Результаты')");
        page.click(".variant-grid button:has-text('Результаты')");
        page.waitForSelector("#modal-container", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(15000));
        assertThat(page.locator("#modal-container")).containsText("Ковалев Дмитрий");

        page.click("button:has-text('Разрешить пересдачу')");
        page.click("button:has-text('Закрыть')");

        // Student attempts test again -> now allowed!
        openTestLinkFresh(testToken);
        page.fill("#student-name-input", "Ковалев Дмитрий");
        page.click("button[type='submit']");

        // Successfully enters task solving screen
        page.waitForSelector(".task-card");
        assertThat(page.locator(".task-card")).isVisible();
    }

    @Test
    void testBlockReattemptWithDifferentNameOnSameDevice() {
        String testToken = createVariantAndExtractToken();

        // First attempt
        openTestLinkFresh(testToken);
        page.fill("#student-name-input", "Петров Алексей");
        page.click("button[type='submit']");

        page.waitForSelector(".task-card");
        page.fill("input[placeholder='Введите ответ']", "-6");
        page.click("button:has-text('Завершить и сдать тест')");
        page.waitForSelector(".score-hero");

        // Attempt again under a DIFFERENT student name on same device
        openTestLinkFresh(testToken);
        page.fill("#student-name-input", "Сидоров Сергей");
        page.click("button[type='submit']");

        // Verify reattempt block error message is shown due to device fingerprint lock
        page.waitForSelector("#st-error");
        assertThat(page.locator("#st-error")).isVisible();
        assertThat(page.locator("#st-error")).containsText("Вы уже сдали этот тест на данном устройстве");
    }
}
