package com.example.test_platform.domain.entity;

import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
@Getter
@Setter
public class Task extends BaseEntity {

    @Column(nullable = false, unique = true, length = 32)
    private String publicId;

    @Column(name = "external_id", length = 64)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskBank taskBank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ExamType examType;

    @Column(nullable = false)
    private Integer taskNumber;

    @Column(nullable = false, length = 300)
    private String subtopic;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<TaskImage> images = new ArrayList<>();

    public void addImage(TaskImage image) {
        images.add(image);
        image.setTask(this);
    }

    public void removeImage(TaskImage image) {
        images.remove(image);
        image.setTask(null);
    }
}
