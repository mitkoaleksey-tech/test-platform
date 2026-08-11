package com.example.test_platform.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SubmitTestAttemptRequest {

    @NotNull
    private Long attemptId;

    private List<StudentAnswerDto> answers = new ArrayList<>();
}
