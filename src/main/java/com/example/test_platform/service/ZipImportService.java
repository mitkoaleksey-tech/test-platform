package com.example.test_platform.service;

import com.example.test_platform.config.StorageProperties;
import com.example.test_platform.domain.entity.Task;
import com.example.test_platform.domain.entity.TaskImage;
import com.example.test_platform.domain.enums.ExamType;
import com.example.test_platform.domain.enums.Subject;
import com.example.test_platform.domain.enums.TaskBank;
import com.example.test_platform.dto.response.ZipImportResultResponse;
import com.example.test_platform.exception.ApiException;
import com.example.test_platform.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZipImportService {

    private final TaskRepository taskRepository;
    private final StorageProperties storageProperties;

    @Transactional
    public ZipImportResultResponse importZipArchive(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Файл архива не предоставлен или пуст");
        }

        List<String> warnings = new ArrayList<>();
        int totalProcessed = 0;
        int createdCount = 0;
        int updatedCount = 0;
        int imagesAttached = 0;

        Path tempDir = null;

        try {
            log.info("Начата распаковка ZIP архива '{}' (размер: {} КБ)", file.getOriginalFilename(), file.getSize() / 1024);
            tempDir = Files.createTempDirectory("zip_import_");
            Path excelPath = null;
            Map<String, Path> imagesMap = new HashMap<>();

            // 1. Распаковка архива во временную папку
            try (InputStream is = file.getInputStream();
                 ZipInputStream zis = new ZipInputStream(is)) {
                
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;

                    String entryName = entry.getName().replace("\\", "/");
                    String lowerName = entryName.toLowerCase();

                    Path targetPath = tempDir.resolve(entryName);
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);

                    if (!targetPath.getFileName().toString().startsWith("~$") && (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls"))) {
                        if (excelPath == null || lowerName.endsWith("tasks.xlsx")) {
                            excelPath = targetPath;
                        }
                    } else if (lowerName.contains("images/") || lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif") || lowerName.endsWith(".webp") || lowerName.endsWith(".svg")) {
                        String fileNameOnly = targetPath.getFileName().toString().toLowerCase();
                        imagesMap.put(fileNameOnly, targetPath);
                    }
                }
            }

            if (excelPath == null || !Files.exists(excelPath)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 
                    "В архиве '" + file.getOriginalFilename() + "' не найден файл таблицы Excel (.xlsx или .xls). " +
                    "Архив должен содержать таблицу задач (tasks.xlsx) и папку с картинками.");
            }

            // 2. Предварительная загрузка существующих задач для исключения N+1 SQL-запросов
            log.info("Загрузка существующих задач из базы данных для быстрого сопоставления...");
            Map<String, Task> existingTasksMap = new HashMap<>();
            for (Task t : taskRepository.findAll()) {
                if (t.getExternalId() != null && !t.getExternalId().isBlank()) {
                    existingTasksMap.putIfAbsent(t.getTaskBank() + ":" + t.getExternalId(), t);
                }
            }
            log.info("Загружено {} существующих задач. Начато чтение Excel файла: {}", 
                    existingTasksMap.size(), excelPath.getFileName());

            List<Task> batchList = new ArrayList<>();
            Map<Path, String> savedImageCache = new HashMap<>();

            // 3. Чтение таблицы tasks.xlsx через Apache POI
            try (Workbook workbook = WorkbookFactory.create(excelPath.toFile())) {
                Sheet sheet = workbook.getSheetAt(0);
                boolean isHeader = true;
                int totalRowsInSheet = sheet.getLastRowNum();

                for (Row row : sheet) {
                    if (isHeader) {
                        isHeader = false; // Пропускаем заголовок таблицы
                        continue;
                    }

                    String extId = getCellValue(row, 0);
                    String subjStr = getCellValue(row, 1);
                    String examStr = getCellValue(row, 2);
                    String bankStr = getCellValue(row, 3);
                    String taskNumStr = getCellValue(row, 4);
                    String subtopicStr = getCellValue(row, 5);
                    String questionStr = getCellValue(row, 6);
                    String imageFilesStr = getCellValue(row, 7);
                    String answerStr = getCellValue(row, 8);

                    if (subjStr.isBlank() && questionStr.isBlank()) {
                        continue; // Пустая строка
                    }

                    totalProcessed++;

                    if (totalProcessed == 1 || totalProcessed % 1000 == 0) {
                        log.info("Импорт ZIP: обработано {} из ~{} строк (создано: {}, обновлено: {}, картинок: {})",
                                totalProcessed, totalRowsInSheet, createdCount, updatedCount, imagesAttached);
                    }

                    // Парсинг перечислений
                    Subject subject = parseSubject(subjStr);
                    ExamType examType = parseExamType(examStr);
                    TaskBank taskBank = parseTaskBank(bankStr);
                    Integer taskNumber = parseTaskNumber(taskNumStr);

                    // Быстрый поиск задачи в O(1) памяти вместо SQL SELECT
                    Task task = null;
                    if (!extId.isBlank()) {
                        task = existingTasksMap.get(taskBank + ":" + extId);
                    }

                    boolean isNew = (task == null);
                    if (isNew) {
                        task = new Task();
                        task.setPublicId("T-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                        task.setTaskBank(taskBank);
                        if (!extId.isBlank()) {
                            task.setExternalId(extId);
                            existingTasksMap.put(taskBank + ":" + extId, task);
                        }
                        createdCount++;
                    } else {
                        updatedCount++;
                    }

                    task.setSubject(subject);
                    task.setExamType(examType);
                    task.setTaskNumber(taskNumber);
                    task.setSubtopic(subtopicStr.isBlank() ? "Общие задания" : subtopicStr);
                    task.setContent(com.example.test_platform.util.FipiTextNormalizer.normalize(questionStr));
                    task.setCorrectAnswer(answerStr);
                    task.setActive(true);

                    // 4. Обработка картинок из папки images
                    if (!isNew && task.getImages() != null) {
                        task.getImages().clear();
                    }
                    if (!imageFilesStr.isBlank()) {
                        // Очистка от JSON-скобок и кавычек: ["file1.png", "file2.png"] -> file1.png, file2.png
                        String cleanedImageStr = imageFilesStr.replace("[", "").replace("]", "").replace("\"", "").replace("'", "");
                        String[] files = cleanedImageStr.split(",");
                        int order = 1;

                        for (String fName : files) {
                            String cleanName = fName.trim().toLowerCase();
                            if (cleanName.isBlank() || cleanName.equals("nan")) continue;

                            Path imgPath = imagesMap.get(cleanName);
                            if (imgPath != null && Files.exists(imgPath)) {
                                String savedFileName;
                                if (savedImageCache.containsKey(imgPath)) {
                                    savedFileName = savedImageCache.get(imgPath);
                                } else {
                                    savedFileName = saveImageToStorage(imgPath);
                                    savedImageCache.put(imgPath, savedFileName);
                                }
                                TaskImage taskImage = new TaskImage();
                                taskImage.setFilePath(savedFileName);
                                taskImage.setOriginalFilename(fName.trim().replace("\"", "").replace("'", ""));
                                taskImage.setWidthPx(storageProperties.getImageTargetWidth());
                                taskImage.setSortOrder(order++);
                                task.addImage(taskImage);
                                imagesAttached++;
                            } else {
                                if (cleanName.contains(".")) {
                                    warnings.add("Строка " + (row.getRowNum() + 1) + ": Файл картинки '" + cleanName + "' не найден в папке /images архива");
                                }
                            }
                        }
                    }

                    batchList.add(task);
                    if (batchList.size() >= 500) {
                        taskRepository.saveAllAndFlush(batchList);
                        batchList.clear();
                    }
                }

                if (!batchList.isEmpty()) {
                    taskRepository.saveAllAndFlush(batchList);
                    batchList.clear();
                }
            }

            log.info("Успешно завершен импорт ZIP! Всего обработано: {}, создано: {}, обновлено: {}, картинок: {}, предупреждений: {}",
                    totalProcessed, createdCount, updatedCount, imagesAttached, warnings.size());

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при импорте ZIP архива", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка обработки архива: " + e.getMessage());
        } finally {
            if (tempDir != null) {
                deleteDirectoryRecursively(tempDir.toFile());
            }
        }

        return ZipImportResultResponse.builder()
                .totalProcessed(totalProcessed)
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .imagesAttachedCount(imagesAttached)
                .warnings(warnings)
                .build();
    }

    private final org.apache.poi.ss.usermodel.DataFormatter dataFormatter = new org.apache.poi.ss.usermodel.DataFormatter();

    private String getCellValue(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return "";
        return dataFormatter.formatCellValue(cell).trim();
    }

    private Subject parseSubject(String str) {
        if (str.isBlank()) return Subject.MATHEMATICS_PROF;
        String upper = str.toUpperCase();
        if (upper.contains("BASE") || upper.contains("БАЗ")) return Subject.MATHEMATICS_BASE;
        if (upper.contains("PROF") || upper.contains("ПРОФ")) return Subject.MATHEMATICS_PROF;
        if (upper.contains("PHYS") || upper.contains("ФИЗ")) return Subject.PHYSICS;
        return Subject.MATHEMATICS;
    }

    private ExamType parseExamType(String str) {
        if (str.isBlank()) return ExamType.EGE;
        String upper = str.toUpperCase();
        if (upper.contains("OGE") || upper.contains("ОГЭ")) return ExamType.OGE;
        return ExamType.EGE;
    }

    private TaskBank parseTaskBank(String str) {
        if (str.isBlank()) return TaskBank.FIPI;
        String upper = str.toUpperCase();
        if (upper.contains("STAT") || upper.contains("СТАТ")) return TaskBank.STATGRAD;
        return TaskBank.FIPI;
    }

    private Integer parseTaskNumber(String str) {
        try {
            int num = Integer.parseInt(str);
            return num > 0 ? num : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private String saveImageToStorage(Path sourcePath) throws Exception {
        Path uploadDir = Path.of(storageProperties.getImagesPath());
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        String ext = getFileExtension(sourcePath.getFileName().toString());
        String newName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path targetPath = uploadDir.resolve(newName);
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        return newName;
    }

    private String getFileExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx >= 0 ? fileName.substring(idx) : ".png";
    }

    private void deleteDirectoryRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteDirectoryRecursively(child);
                }
            }
        }
        file.delete();
    }
}
