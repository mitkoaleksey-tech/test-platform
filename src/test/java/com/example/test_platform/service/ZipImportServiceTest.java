package com.example.test_platform.service;

import com.example.test_platform.domain.entity.Task;
import com.example.test_platform.exception.ApiException;
import com.example.test_platform.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ZipImportServiceTest {

    @Autowired
    private ZipImportService zipImportService;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void testImportZipWith14Columns() throws Exception {
        File zipFile = new File("output/images.zip");
        if (!zipFile.exists()) {
            System.out.println("No output/images.zip found, skipping test.");
            return;
        }

        try (FileInputStream fis = new FileInputStream(zipFile)) {
            MockMultipartFile multipartFile = new MockMultipartFile(
                    "file",
                    "images.zip",
                    "application/zip",
                    fis
            );

            try {
                zipImportService.importZipArchive(multipartFile);
            } catch (ApiException e) {
                System.out.println(">>> API EXCEPTION: " + e.getMessage());
                throw e;
            } catch (Exception e) {
                System.out.println(">>> EXCEPTION: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }

        List<Task> tasks = taskRepository.findAll();
        assertFalse(tasks.isEmpty(), "Tasks repository should not be empty after ZIP import");

        long emptyContentCount = tasks.stream().filter(t -> t.getContent() == null || t.getContent().isBlank()).count();
        System.out.println("Total imported tasks: " + tasks.size());
        System.out.println("Tasks with empty content: " + emptyContentCount);

        assertEquals(0, emptyContentCount, "All imported tasks MUST have non-empty question content!");
    }
}
