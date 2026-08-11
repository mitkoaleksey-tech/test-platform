package com.example.test_platform.repository;

import com.example.test_platform.domain.entity.TaskImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskImageRepository extends JpaRepository<TaskImage, Long> {

    List<TaskImage> findByTaskIdOrderBySortOrderAsc(Long taskId);
}
