package com.example.test_platform.service;

import com.example.test_platform.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FipiCsvImporterTest {

    @Autowired
    private FipiCsvImporterService importerService;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    @DisplayName("Проверка работы импортера ФИПИ CSV из папки archive")
    void testImportFromArchive() {
        File csvFile = new File("archive/tasks.csv");
        File assetsDir = new File("archive/assets");

        if (csvFile.exists()) {
            FipiCsvImporterService.ImportResult result = importerService.importCsv(csvFile, assetsDir, 50);
            assertThat(result.totalProcessed()).isGreaterThan(0);
            assertThat(taskRepository.count()).isGreaterThan(0);
        }
    }
}
