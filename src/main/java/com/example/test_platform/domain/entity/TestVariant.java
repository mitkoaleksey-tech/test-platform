package com.example.test_platform.domain.entity;

import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_variants")
@Getter
@Setter
public class TestVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 64)
    private String accessToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ExamType examType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskBank taskBank;

    @OneToMany(mappedBy = "testVariant", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<TestVariantTask> variantTasks = new ArrayList<>();

    public void addVariantTask(TestVariantTask variantTask) {
        variantTasks.add(variantTask);
        variantTask.setTestVariant(this);
    }

    public void removeVariantTask(TestVariantTask variantTask) {
        variantTasks.remove(variantTask);
        variantTask.setTestVariant(null);
    }
}
