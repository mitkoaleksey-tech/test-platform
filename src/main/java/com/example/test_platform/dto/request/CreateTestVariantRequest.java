package com.example.test_platform.dto.request;

import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateTestVariantRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotNull
    private ExamType examType;

    @NotNull
    private Subject subject;

    @NotNull
    private TaskBank taskBank;

    @NotEmpty
    private List<Long> taskIds;
}
