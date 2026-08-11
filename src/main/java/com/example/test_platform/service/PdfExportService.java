package com.example.test_platform.service;

import com.example.test_platform.config.StorageProperties;
import com.example.test_platform.domain.entity.Task;
import com.example.test_platform.domain.entity.TaskImage;
import com.example.test_platform.domain.entity.TestVariant;
import com.example.test_platform.domain.entity.TestVariantTask;
import com.example.test_platform.exception.ApiException;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final StorageProperties storageProperties;

    public byte[] generateTestVariantPdf(TestVariant variant) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            BaseFont baseFont = loadCyrillicFont();
            Font titleFont = (baseFont != null)
                    ? new Font(baseFont, 18, Font.BOLD, Color.BLACK)
                    : new Font(Font.HELVETICA, 18, Font.BOLD, Color.BLACK);
            Font headerFont = (baseFont != null)
                    ? new Font(baseFont, 11, Font.NORMAL, Color.DARK_GRAY)
                    : new Font(Font.HELVETICA, 11, Font.NORMAL, Color.DARK_GRAY);
            Font taskHeaderFont = (baseFont != null)
                    ? new Font(baseFont, 12, Font.BOLD, new Color(30, 64, 175))
                    : new Font(Font.HELVETICA, 12, Font.BOLD, Color.BLUE);
            Font bodyFont = (baseFont != null)
                    ? new Font(baseFont, 11, Font.NORMAL, Color.BLACK)
                    : new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);

            // Title
            Paragraph title = new Paragraph(variant.getTitle(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8);
            document.add(title);

            // Meta
            Paragraph meta = new Paragraph(
                    String.format("Предмет: %s | Экзамен: %s | Банк: %s",
                            variant.getSubject() != null ? variant.getSubject().getDisplayName() : "—",
                            variant.getExamType() != null ? variant.getExamType().name() : "—",
                            variant.getTaskBank() != null ? variant.getTaskBank().name() : "—"),
                    headerFont
            );
            meta.setAlignment(Element.ALIGN_CENTER);
            meta.setSpacingAfter(15);
            document.add(meta);

            // Student Field Line
            Paragraph studentField = new Paragraph("ФИО ученика: _______________________   Дата: ____________", headerFont);
            studentField.setSpacingAfter(20);
            document.add(studentField);

            // Tasks
            List<TestVariantTask> tasks = variant.getVariantTasks() != null ? variant.getVariantTasks().stream()
                    .sorted(Comparator.comparingInt(TestVariantTask::getSortOrder))
                    .toList() : List.of();

            for (int i = 0; i < tasks.size(); i++) {
                TestVariantTask variantTask = tasks.get(i);
                Task task = variantTask.getTask();

                Paragraph taskHeader = new Paragraph(
                        String.format("Задание №%d. [%s] (ID: %s)", i + 1, task.getSubtopic(), task.getPublicId()),
                        taskHeaderFont
                );
                taskHeader.setSpacingBefore(10);
                taskHeader.setSpacingAfter(5);
                document.add(taskHeader);

                String pdfFormattedContent = com.example.test_platform.util.FipiTextNormalizer.formatForPdf(task.getContent());
                Paragraph taskBody = new Paragraph(pdfFormattedContent, bodyFont);
                taskBody.setSpacingAfter(10);
                document.add(taskBody);

                // Task images
                if (task.getImages() != null && storageProperties != null && storageProperties.getImagesPath() != null) {
                    for (TaskImage taskImage : task.getImages()) {
                        Path imagePath = Path.of(storageProperties.getImagesPath()).resolve(taskImage.getFilePath());
                        if (Files.exists(imagePath)) {
                            try {
                                Image pdfImg = Image.getInstance(imagePath.toAbsolutePath().toString());
                                pdfImg.scaleToFit(500, 400);
                                pdfImg.setAlignment(Element.ALIGN_CENTER);
                                pdfImg.setSpacingAfter(10);
                                document.add(pdfImg);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }

            document.close();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            log.error("Failed to generate PDF document", exception);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate PDF document: " + exception.getMessage());
        }
    }

    private BaseFont loadCyrillicFont() {
        try {
            byte[] fontBytes = null;
            try (InputStream is = getClass().getResourceAsStream("/fonts/arial.ttf")) {
                if (is != null) {
                    fontBytes = is.readAllBytes();
                }
            }

            if (fontBytes == null) {
                Path winPath = Path.of("C:/Windows/Fonts/arial.ttf");
                if (Files.exists(winPath)) {
                    fontBytes = Files.readAllBytes(winPath);
                }
            }

            if (fontBytes != null) {
                return BaseFont.createFont("arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
            }
        } catch (Exception e) {
            log.warn("Could not load custom Cyrillic TTF font, falling back to standard font", e);
        }
        return null;
    }
}

