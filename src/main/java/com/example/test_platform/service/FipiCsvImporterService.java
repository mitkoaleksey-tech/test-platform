package com.example.test_platform.service;

import com.example.test_platform.config.StorageProperties;
import com.example.test_platform.domain.entity.Task;
import com.example.test_platform.domain.entity.TaskImage;
import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import com.example.test_platform.repository.TaskImageRepository;
import com.example.test_platform.repository.TaskRepository;
import com.example.test_platform.util.FipiTextNormalizer;
import com.example.test_platform.util.TaskPublicIdGenerator;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FipiCsvImporterService {

    private final TaskRepository taskRepository;
    private final TaskImageRepository taskImageRepository;
    private final StorageProperties storageProperties;
    private final EntityManager entityManager;

    public record ImportResult(int totalProcessed, int insertedCount, int skippedDuplicatesCount, int errorsCount) {}

    public ImportResult importCsv(File csvFile, File imagesSourceDir) {
        return importCsv(csvFile, imagesSourceDir, 0);
    }

    @Transactional
    public ImportResult importCsv(File csvFile, File imagesSourceDir, int maxRecords) {
        if (!csvFile.exists()) {
            throw new IllegalArgumentException("CSV файл не найден: " + csvFile.getAbsolutePath());
        }

        if (imagesSourceDir == null || !imagesSourceDir.exists()) {
            imagesSourceDir = new File(csvFile.getParentFile(), "assets");
        }

        int processed = 0;
        int inserted = 0;
        int skipped = 0;
        int errors = 0;

        // Кэш имеющихся externalId в RAM для мгновенной O(1) проверки
        Set<String> existingIds = new HashSet<>(taskRepository.findExternalIdsByTaskBank(TaskBank.FIPI));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new ImportResult(0, 0, 0, 0);
            }

            List<String> headers = parseCsvRow(headerLine, reader);
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                headerMap.put(headers.get(i).trim().toLowerCase(), i);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                if (maxRecords > 0 && inserted >= maxRecords) {
                    break;
                }
                processed++;

                try {
                    List<String> cols = parseCsvRow(line, reader);
                    String source = getColValue(cols, headerMap, "source");
                    String subjectCode = getColValue(cols, headerMap, "subject_code");
                    String subjectStr = getColValue(cols, headerMap, "subject");

                    // 1. Фильтр: только источник FIPI
                    if (!source.isBlank() && !source.equalsIgnoreCase("fipi")) {
                        continue;
                    }

                    // 2. Фильтр: только Математика (math, math_base, math_prof)
                    if (!isMathSubject(subjectCode, subjectStr)) {
                        continue;
                    }

                    String examStr = getColValue(cols, headerMap, "exam");
                    ExamType examType = parseExamType(examStr);
                    Subject subject = parseSubject(subjectCode, subjectStr, examType);

                    String rawId = getColValue(cols, headerMap, "id");
                    String fipiId = getColValue(cols, headerMap, "fipi_id");
                    String externalId = !fipiId.isBlank() ? fipiId : (!rawId.isBlank() ? "fipi_" + rawId : "");

                    // Проверка дубликатов в памяти RAM
                    if (!externalId.isBlank() && existingIds.contains(externalId)) {
                        skipped++;
                        continue;
                    }

                    String taskNumberStr = getColValue(cols, headerMap, "task_number");
                    String codifierCodesStr = getColValue(cols, headerMap, "codifier_codes");
                    String question = getColValue(cols, headerMap, "question");
                    String context = getColValue(cols, headerMap, "context");
                    String answer = getColValue(cols, headerMap, "answer");
                    String imageFilesStr = getColValue(cols, headerMap, "image_files");
                    if (imageFilesStr.isBlank()) {
                        imageFilesStr = getColValue(cols, headerMap, "images");
                    }

                    String content = question;
                    if (!context.isBlank()) {
                        content = content.isBlank() ? context : content + "\n\n" + context;
                    }

                    if (content.isBlank()) {
                        errors++;
                        continue;
                    }

                    int taskNumber = parseTaskNumber(taskNumberStr, codifierCodesStr);

                    Task task = new Task();
                    task.setPublicId(generateUniquePublicId());
                    task.setExternalId(externalId);
                    task.setTaskBank(TaskBank.FIPI);
                    task.setSubject(subject);
                    task.setExamType(examType);
                    task.setTaskNumber(taskNumber);
                    task.setSubtopic(subject.getDisplayName() + " (Задание №" + taskNumber + ")");
                    task.setContent(FipiTextNormalizer.normalize(content));
                    task.setCorrectAnswer(answer);
                    task.setActive(true);

                    Task savedTask = taskRepository.save(task);
                    inserted++;
                    if (!externalId.isBlank()) {
                        existingIds.add(externalId);
                    }

                    // Импорт картинок
                    if (!imageFilesStr.isBlank() && imagesSourceDir.exists()) {
                        String[] imgFiles = imageFilesStr.split("[,;]");
                        int sortOrder = 0;
                        for (String imgFilename : imgFiles) {
                            imgFilename = imgFilename.trim().replaceAll("[\"'\\[\\]]", "");
                            if (imgFilename.isBlank()) continue;

                            File sourceImg = new File(imagesSourceDir, imgFilename);
                            if (sourceImg.exists()) {
                                processAndAttachImage(savedTask, sourceImg, imgFilename, sortOrder++);
                            }
                        }
                    }

                    // Очистка кэша JPA каждые 50 записей для постоянной наносекундной скорости
                    if (inserted % 50 == 0) {
                        try {
                            entityManager.flush();
                            entityManager.clear();
                        } catch (Exception ignored) {}
                    }
                } catch (Exception e) {
                    if (processed <= 10 || processed % 1000 == 0) {
                        log.warn("Ошибка строки CSV #{}: {}", processed, e.getMessage());
                    }
                    errors++;
                }

                if (processed % 2000 == 0) {
                    log.info("Прогресс импорта ФИПИ: прочитано {} строк | Загружено Математика: {} | Пропущено: {}", processed, inserted, skipped);
                }
            }
        } catch (Exception e) {
            log.error("Ошибка чтения CSV файла: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка чтения CSV файла", e);
        }

        log.info("=== ИМПОРТ ФИПИ ЗАВЕРШЕН ===");
        log.info("Всего обработано: {}, Загружено задач по математике: {}, Пропущено дублей: {}, Ошибок: {}",
                processed, inserted, skipped, errors);
        return new ImportResult(processed, inserted, skipped, errors);
    }

    private boolean isMathSubject(String subjectCode, String subjectStr) {
        if (subjectCode != null && subjectCode.toLowerCase().startsWith("math")) {
            return true;
        }
        if (subjectStr != null && subjectStr.toLowerCase().contains("математ")) {
            return true;
        }
        return false;
    }

    private Subject parseSubject(String subjectCode, String subjectStr, ExamType examType) {
        if (subjectCode != null && !subjectCode.isBlank()) {
            String lowerCode = subjectCode.toLowerCase();
            if (lowerCode.equals("math_base")) return Subject.MATHEMATICS_BASE;
            if (lowerCode.equals("math_prof")) return Subject.MATHEMATICS_PROF;
            if (lowerCode.equals("math")) {
                return examType == ExamType.OGE ? Subject.MATHEMATICS : Subject.MATHEMATICS_PROF;
            }
        }

        if (subjectStr != null && !subjectStr.isBlank()) {
            String lower = subjectStr.toLowerCase();
            if (lower.contains("базовый") || lower.contains("база")) return Subject.MATHEMATICS_BASE;
            if (lower.contains("профильный") || lower.contains("профиль")) return Subject.MATHEMATICS_PROF;
            if (lower.contains("математ")) {
                return examType == ExamType.OGE ? Subject.MATHEMATICS : Subject.MATHEMATICS_PROF;
            }
        }

        return Subject.MATHEMATICS;
    }

    private ExamType parseExamType(String examStr) {
        if (examStr != null && (examStr.equalsIgnoreCase("ОГЭ") || examStr.equalsIgnoreCase("OGE"))) {
            return ExamType.OGE;
        }
        return ExamType.EGE;
    }

    private int parseTaskNumber(String taskNumberStr, String codifierCodesStr) {
        try {
            if (taskNumberStr != null && !taskNumberStr.isBlank()) {
                return Integer.parseInt(taskNumberStr.trim());
            }
        } catch (Exception ignored) {}

        if (codifierCodesStr != null && !codifierCodesStr.isBlank()) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"?(\\d+)").matcher(codifierCodesStr);
            if (m.find()) {
                try {
                    int num = Integer.parseInt(m.group(1));
                    if (num >= 1 && num <= 30) {
                        return num;
                    }
                } catch (Exception ignored) {}
            }
        }
        return 1;
    }

    private String getColValue(List<String> cols, Map<String, Integer> headerMap, String colName) {
        Integer idx = headerMap.get(colName.toLowerCase());
        if (idx != null && idx < cols.size()) {
            String val = cols.get(idx);
            return val != null ? val.trim() : "";
        }
        return "";
    }

    private void processAndAttachImage(Task task, File sourceImg, String originalFilename, int sortOrder) {
        try {
            String extension = getFileExtension(originalFilename);
            String storageFilename = UUID.randomUUID() + extension;
            File targetDir = new File(storageProperties.getImagesPath());
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            File targetFile = new File(targetDir, storageFilename);
            Files.copy(sourceImg.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            int widthPx = 0;
            try {
                BufferedImage bimg = ImageIO.read(targetFile);
                if (bimg != null) {
                    widthPx = bimg.getWidth();
                }
            } catch (Exception ex) {
                log.warn("Ширина картинки {}: {}", storageFilename, ex.getMessage());
            }

            TaskImage image = new TaskImage();
            image.setTask(task);
            image.setFilePath(storageFilename);
            image.setOriginalFilename(originalFilename);
            image.setWidthPx(widthPx);
            image.setSortOrder(sortOrder);

            task.addImage(image);
            taskImageRepository.save(image);
        } catch (Exception ex) {
            log.error("Ошибка картинки {}: {}", originalFilename, ex.getMessage());
        }
    }

    private String generateUniquePublicId() {
        String publicId;
        do {
            publicId = TaskPublicIdGenerator.generate();
        } while (taskRepository.existsByPublicId(publicId));
        return publicId;
    }

    private String getFileExtension(String filename) {
        int dotIdx = filename.lastIndexOf('.');
        return dotIdx >= 0 ? filename.substring(dotIdx) : ".png";
    }

    /**
     * Парсинг одной строки RFC-4180 CSV формата с поддержкой переносов строк внутри кавычек
     */
    private static List<String> parseCsvRow(String line, BufferedReader reader) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        String currentLine = line;
        int i = 0;
        while (currentLine != null) {
            while (i < currentLine.length()) {
                char c = currentLine.charAt(i);
                if (inQuotes) {
                    if (c == '"') {
                        if (i + 1 < currentLine.length() && currentLine.charAt(i + 1) == '"') {
                            sb.append('"');
                            i++;
                        } else {
                            inQuotes = false;
                        }
                    } else {
                        sb.append(c);
                    }
                } else {
                    if (c == '"') {
                        inQuotes = true;
                    } else if (c == ',') {
                        fields.add(sb.toString());
                        sb.setLength(0);
                    } else {
                        sb.append(c);
                    }
                }
                i++;
            }

            if (inQuotes) {
                sb.append("\n");
                currentLine = reader.readLine();
                i = 0;
            } else {
                break;
            }
        }
        fields.add(sb.toString());
        return fields;
    }
}
