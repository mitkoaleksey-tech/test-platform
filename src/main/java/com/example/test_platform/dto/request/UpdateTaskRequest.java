package com.example.test_platform.dto.request;

import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTaskRequest {

    @NotNull
    private Subject subject;

    @NotNull
    private TaskBank taskBank;

    @NotNull
    private ExamType examType;

    @NotNull
    @Min(1)
    private Integer taskNumber;

    @NotBlank
    @Size(max = 300)
    private String subtopic;

    @NotBlank
    private String content;

    private String correctAnswer;

    private boolean active = true;
}
