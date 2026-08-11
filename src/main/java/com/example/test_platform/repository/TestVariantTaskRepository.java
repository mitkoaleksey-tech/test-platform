package com.example.test_platform.repository;

import com.example.test_platform.domain.entity.TestVariantTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestVariantTaskRepository extends JpaRepository<TestVariantTask, Long> {

    List<TestVariantTask> findByTestVariantIdOrderBySortOrderAsc(Long testVariantId);

    void deleteByTestVariantId(Long testVariantId);

    void deleteByTaskId(Long taskId);
}
