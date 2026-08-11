package com.example.test_platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartTestAttemptRequest {

    @NotBlank
    @Size(max = 200)
    @Pattern(regexp = "^[а-яА-ЯёЁ\\s-]+$", message = "Имя ученика должно содержать только русский алфавит")
    private String studentName;

    private String browserFingerprint;
}

