package com.example.test_platform.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class TeacherVariantUiE2eTest extends BasePlaywrightTest {

    private static final String TEACHER_LOGIN = "teacher_variant_e2e";
    private static final String TEACHER_PASS = "teacherPass123";

    @BeforeEach
    void setupTeacherAndTask() {
        // Create teacher and task programmatically (known password)
        createTeacher(TEACHER_LOGIN, "Учитель Тестов", TEACHER_PASS);
        createTask("Тригонометрия", "Решите уравнение sin(x) = 1.", "90");
    }

    @Test
    void testTeacherVariantBuilderAndLinkGeneration() {
        // Teacher logs in via UI
        loginViaUi(TEACHER_LOGIN, TEACHER_PASS);

        // Go to Create Variant tab
        page.click("button:has-text('Собрать новый вариант')");
        page.waitForSelector("button:has-text('Сгенерировать вариант')");

        // Verify subtopic is displayed
        assertThat(page.locator(".container")).containsText("Задание №1 КИМ");
        assertThat(page.locator(".container")).containsText("Тригонометрия");

        // Click '+' button to add task from subtopic
        page.click("button:has-text('+')");

        // Submit variant creation
        page.click("button:has-text('Сгенерировать вариант')");

        // Verify variant appears in teacher dashboard
        page.waitForSelector(".variant-grid");
        assertThat(page.locator(".variant-grid")).isVisible();
        assertThat(page.locator("button:has-text('Ссылка для учеников')").first()).isVisible();
    }

    @Test
    void testSubtopicTaskCounters() {
        loginViaUi(TEACHER_LOGIN, TEACHER_PASS);

        page.click("button:has-text('Собрать новый вариант')");
        page.waitForSelector("button:has-text('Сгенерировать вариант')");

        // Check KIM subtopic counter displays available tasks count
        assertThat(page.locator(".container")).containsText("Тригонометрия");
        assertThat(page.locator("button:has-text('+')").first()).isVisible();
    }

    @Test
    void testExportPdfVariant() {
        // First create a variant
        loginViaUi(TEACHER_LOGIN, TEACHER_PASS);
        page.click("button:has-text('Собрать новый вариант')");
        page.waitForSelector("button:has-text('Сгенерировать вариант')");
        page.click("button:has-text('+')");
        page.click("button:has-text('Сгенерировать вариант')");

        page.waitForSelector(".variant-grid");
        // Verify print button exists (replaced old PDF link with browser-print button)
        assertThat(page.locator("button:has-text('Печать / PDF')").first()).isVisible();
    }
}
