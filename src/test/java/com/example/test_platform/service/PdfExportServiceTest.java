package com.example.test_platform.service;

import com.example.test_platform.config.StorageProperties;
import com.example.test_platform.domain.entity.Task;
import com.example.test_platform.domain.entity.TestVariant;
import com.example.test_platform.domain.entity.TestVariantTask;
import com.example.test_platform.domain.entity.User;
import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import com.example.test_platform.domain.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfExportServiceTest {

    @Test
    void testGenerateTestVariantPdfWithCyrillic_ReturnsNonEmptyBytes() {
        StorageProperties properties = new StorageProperties();
        properties.setImagesPath("./storage/images");

        PdfExportService pdfExportService = new PdfExportService(properties);

        User teacher = new User();
        teacher.setLogin("teacher1");
        teacher.setDisplayName("Учитель Математики");
        teacher.setRole(UserRole.TEACHER);

        Task task1 = new Task();
        task1.setPublicId("TASK-RUS01");
        task1.setSubject(Subject.MATHEMATICS);
        task1.setExamType(ExamType.EGE);
        task1.setTaskBank(TaskBank.FIPI);
        task1.setTaskNumber(1);
        task1.setSubtopic("Планиметрия и геометрия");
        task1.setContent("Найдите площадь треугольника с основанием $a = 10$ и высотой $h = 5$.");
        task1.setCorrectAnswer("25");

        TestVariant variant = new TestVariant();
        variant.setTitle("Тренировочный вариант по математике №1");
        variant.setSubject(Subject.MATHEMATICS);
        variant.setExamType(ExamType.EGE);
        variant.setTaskBank(TaskBank.FIPI);
        variant.setAccessToken("test-token-123");
        variant.setTeacher(teacher);

        TestVariantTask vt = new TestVariantTask();
        vt.setTestVariant(variant);
        vt.setTask(task1);
        vt.setSortOrder(1);

        variant.setVariantTasks(List.of(vt));

        byte[] pdfBytes = pdfExportService.generateTestVariantPdf(variant);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF byte array should contain generated content");
    }
}
