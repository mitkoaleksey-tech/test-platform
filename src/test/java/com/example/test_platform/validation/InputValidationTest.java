package com.example.test_platform.validation;

import com.example.test_platform.dto.request.CreateUserRequest;
import com.example.test_platform.dto.request.LoginRequest;
import com.example.test_platform.dto.request.StartTestAttemptRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void testLoginStrictlyEnglish_Success() {
        CreateUserRequest request = new CreateUserRequest();
        request.setLogin("teacher_user1");
        request.setDisplayName("Петр Иванов");

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Latin login should be valid");
    }

    @Test
    void testLoginWithCyrillic_FailsValidation() {
        CreateUserRequest request = new CreateUserRequest();
        request.setLogin("учитель1");
        request.setDisplayName("Петр Иванов");

        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Cyrillic login must fail validation");
    }

    @Test
    void testPasswordWithCyrillic_FailsValidation() {
        LoginRequest request = new LoginRequest();
        request.setLogin("admin");
        request.setPassword("Пароль123!");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Cyrillic password must fail validation");
    }

    @Test
    void testStudentNameStrictlyRussian_Success() {
        StartTestAttemptRequest request = new StartTestAttemptRequest();
        request.setStudentName("Иванов Иван Петрович");
        request.setBrowserFingerprint("fp-12345");

        Set<ConstraintViolation<StartTestAttemptRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Russian student name should be valid");
    }

    @Test
    void testStudentNameWithLatin_FailsValidation() {
        StartTestAttemptRequest request = new StartTestAttemptRequest();
        request.setStudentName("John Doe");
        request.setBrowserFingerprint("fp-12345");

        Set<ConstraintViolation<StartTestAttemptRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Latin student name must fail validation");
    }
}
