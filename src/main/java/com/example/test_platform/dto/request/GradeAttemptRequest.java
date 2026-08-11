package com.example.test_platform.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GradeAttemptRequest {

    private List<TaskGrade> taskGrades;

    @Getter
    @Setter
    public static class TaskGrade {
        private Long taskId;
        private Integer score;
    }
}
