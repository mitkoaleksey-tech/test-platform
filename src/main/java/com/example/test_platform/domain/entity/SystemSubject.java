package com.example.test_platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "system_subjects")
@Getter
@Setter
public class SystemSubject extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false)
    private Integer totalTasks = 20;

    public Integer getTotalTasks() {
        if (totalTasks != null && totalTasks > 0 && totalTasks != 27) {
            return totalTasks;
        }
        return getDefaultTotalTasksByName();
    }

    private int getDefaultTotalTasksByName() {
        if (name == null) return 20;
        String upper = name.toUpperCase();
        if (upper.equals("MATHEMATICS")) return 25;
        if (upper.equals("MATHEMATICS_BASE")) return 21;
        if (upper.equals("MATHEMATICS_PROF")) return 19;
        return 20;
    }
}
