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
        name = "students",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_teacher_student_identity",
                columnNames = {"teacher_id", "display_name", "browser_fingerprint"}
        )
)
@Getter
@Setter
public class Student extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(nullable = false, length = 200)
    private String displayName;

    @Column(nullable = false, length = 128)
    private String browserFingerprint;
}
