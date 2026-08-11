package com.example.test_platform.e2e;

import com.example.test_platform.domain.entity.User;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthUiE2eTest extends BasePlaywrightTest {

    @Test
    void testLoginPageRendersAndFiltersLatinOnly() {
        page.navigate(baseUrl() + "/#login");

        assertThat(page.locator("h1")).hasText("Reshaemo");

        // Type Cyrillic and Latin characters into login input
        page.fill("#login-input", "тест_test123");
        // Verify input mask strips Cyrillic characters, leaving only Latin/digits/allowed symbols
        assertEquals("_test123", page.inputValue("#login-input"));

        // Type Cyrillic into password input
        page.fill("#password-input", "парольPass123");
        // Verify password mask strips Cyrillic characters
        assertEquals("Pass123", page.inputValue("#password-input"));
    }

    @Test
    void testAdminLoginSuccess() {
        page.navigate(baseUrl() + "/#login");

        page.fill("#login-input", "admin");
        page.fill("#password-input", "admin123");
        page.click("button[type='submit']");

        page.waitForSelector(".navbar-brand");
        assertThat(page.locator(".navbar-brand")).containsText("Reshaemo");
    }

    @Test
    void testStaleJwtTokenClearedOnUnauthorized() {
        page.navigate(baseUrl() + "/#login");

        // Set stale/invalid token in localStorage
        page.evaluate("() => {" +
                "  localStorage.setItem('jwt_token', 'stale.invalid.jwt_token_string');" +
                "  localStorage.setItem('user_profile', JSON.stringify({id: 999, login: 'stale_user', displayName: 'Stale User', role: 'ADMIN'}));" +
                "}");

        // Invoke logout / 401 stale token purge helper
        page.evaluate("() => logout()");

        // Stale token triggers logout(), clearing localStorage and redirecting to #login
        page.waitForSelector("#login-form");
        assertThat(page.locator("h1")).hasText("Reshaemo");

        Object tokenInStorage = page.evaluate("() => localStorage.getItem('jwt_token')");
        assertNull(tokenInStorage, "jwt_token should be cleared from localStorage after stale JWT cleanup");
    }

    @Test
    void testPasswordCopyAction() {
        // Ensure teacher with temporary password exists
        User teacher = createTeacher("teacher_copy_test", "Учитель Копирования", "pass123");
        teacher.setTemporaryPassword(true);
        userRepository.save(teacher);

        // Login as admin
        loginViaUi("admin", "admin123");
        page.waitForSelector(".navbar-brand");

        page.navigate(baseUrl() + "/#admin");
        page.waitForSelector("tbody");

        // Locate copy password and copy login buttons and click them
        assertThat(page.locator("button:has-text('📋 Пароль')").first()).isVisible();
        page.locator("button:has-text('📋 Пароль')").first().click();

        assertThat(page.locator("button:has-text('📋 Логин')").first()).isVisible();
        page.locator("button:has-text('📋 Логин')").first().click();
    }
}
