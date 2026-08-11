package com.example.test_platform.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentAnswerDto {

    @NotNull
    private Long taskId;

    private String answer;
}
