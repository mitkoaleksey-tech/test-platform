package com.example.test_platform.config;

import com.example.test_platform.service.FipiCsvImporterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
@Profile("import")
@RequiredArgsConstructor
public class FipiCsvImportRunner implements CommandLineRunner {

    private final FipiCsvImporterService importerService;

    @Override
    public void run(String... args) throws Exception {
        log.info("==========================================================");
        log.info(">>> НАЧАЛО АВТОМАТИЧЕСКОЙ ЗАЛИВКИ ЗАДАЧ ФИПИ В БАЗУ ДАННЫХ <<<");
        log.info("==========================================================");

        File csvFile = new File("archive/tasks.csv");
        File assetsDir = new File("archive/assets");

        if (csvFile.exists()) {
            FipiCsvImporterService.ImportResult result = importerService.importCsv(csvFile, assetsDir);
            log.info("==========================================================");
            log.info(">>> УСПЕШНО ЗАВЕРШЕНО! <<<");
            log.info("Обработано записей: {}", result.totalProcessed());
            log.info("Загружено новых задач в БД: {}", result.insertedCount());
            log.info("Пропущено имеющихся дубликатов: {}", result.skippedDuplicatesCount());
            log.info("Ошибок в файле: {}", result.errorsCount());
            log.info("==========================================================");
        } else {
            log.error("ОШИБКА: Файл archive/tasks.csv не найден по пути: {}", csvFile.getAbsolutePath());
        }
    }
}
