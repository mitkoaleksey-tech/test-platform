package com.example.test_platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "kim_task_settings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_subject_exam_number",
                columnNames = {"subject_name", "exam_name", "task_number"}
        )
)
@Getter
@Setter
public class KimTaskSetting extends BaseEntity {

    @Column(name = "subject_name", nullable = false, length = 100)
    private String subjectName;

    @Column(name = "exam_name", nullable = false, length = 100)
    private String examName;

    @Column(name = "task_number", nullable = false)
    private Integer taskNumber;

    @Column(nullable = false)
    private Integer maxScore = 1;

    public Integer getMaxScore() {
        return maxScore != null && maxScore > 0 ? maxScore : 1;
    }
}
