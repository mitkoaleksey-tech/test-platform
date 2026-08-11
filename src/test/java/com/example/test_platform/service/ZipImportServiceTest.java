package com.example.test_platform.service;

import com.example.test_platform.dto.response.ZipImportResultResponse;
import com.example.test_platform.repository.TaskRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ZipImportServiceTest {

    @Autowired
    private ZipImportService zipImportService;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void testImportZipArchiveWithXlsxAndImages() throws Exception {
        // 1. Создаем тестовую книгу Excel с задачами
        ByteArrayOutputStream excelBaos = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Tasks");

            // Header row
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("external_id");
            header.createCell(1).setCellValue("subject");
            header.createCell(2).setCellValue("exam_type");
            header.createCell(3).setCellValue("task_bank");
            header.createCell(4).setCellValue("task_number");
            header.createCell(5).setCellValue("subtopic");
            header.createCell(6).setCellValue("question");
            header.createCell(7).setCellValue("image_files");
            header.createCell(8).setCellValue("correct_answer");

            // Row 1
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("ZIP_TEST_101");
            r1.createCell(1).setCellValue("MATHEMATICS_PROF");
            r1.createCell(2).setCellValue("EGE");
            r1.createCell(3).setCellValue("FIPI");
            r1.createCell(4).setCellValue("1");
            r1.createCell(5).setCellValue("Планиметрия");
            r1.createCell(6).setCellValue("Найдите площадь треугольника ABC.");
            r1.createCell(7).setCellValue("triangle.png");
            r1.createCell(8).setCellValue("24");

            workbook.write(excelBaos);
        }

        // 2. Упаковываем Excel и тестовую картинку в ZIP архив
        ByteArrayOutputStream zipBaos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipBaos)) {
            // tasks.xlsx
            zos.putNextEntry(new ZipEntry("tasks.xlsx"));
            zos.write(excelBaos.toByteArray());
            zos.closeEntry();

            // images/triangle.png
            zos.putNextEntry(new ZipEntry("images/triangle.png"));
            zos.write("FAKE_IMAGE_BYTES_12345".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "math_import_test.zip",
                "application/zip",
                zipBaos.toByteArray()
        );

        // 3. Вызываем сервис импорта
        ZipImportResultResponse result = zipImportService.importZipArchive(zipFile);

        assertNotNull(result);
        assertEquals(1, result.getTotalProcessed());
        assertEquals(1, result.getCreatedCount());
        assertEquals(1, result.getImagesAttachedCount());
        assertTrue(result.getWarnings().isEmpty());

        // 4. Проверяем сохраненную задачу в БД
        var taskOpt = taskRepository.findByTaskBankAndExternalId(com.example.test_platform.domain.enums.TaskBank.FIPI, "ZIP_TEST_101");
        assertTrue(taskOpt.isPresent());

        var task = taskOpt.get();
        assertEquals("Планиметрия", task.getSubtopic());
        assertEquals("24", task.getCorrectAnswer());
        assertEquals(1, task.getImages().size());
        assertEquals("triangle.png", task.getImages().get(0).getOriginalFilename());
    }
}
