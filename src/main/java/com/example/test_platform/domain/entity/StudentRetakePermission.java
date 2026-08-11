package com.example.test_platform.domain.entity;

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
        name = "student_retake_permissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_variant_student_retake",
                columnNames = {"test_variant_id", "student_id"}
        )
)
@Getter
@Setter
public class StudentRetakePermission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_variant_id", nullable = false)
    private TestVariant testVariant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private boolean retakeAllowed = true;
}
