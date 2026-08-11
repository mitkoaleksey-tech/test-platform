package com.example.test_platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "student_answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attempt_task_answer",
                columnNames = {"attempt_id", "task_id"}
        )
)
@Getter
@Setter
public class StudentAnswer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private TestAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(columnDefinition = "TEXT")
    private String answerText;

    @Column(nullable = false)
    private boolean correct = false;

    @Column
    private Integer manualScore;
}
