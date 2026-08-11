package com.example.test_platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Логин должен содержать только латинские буквы, цифры и символы _ . -")
    private String login;

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+$", message = "Пароль должен содержать только латинские буквы, цифры и спецсимволы, без кириллицы")
    private String password;
}

