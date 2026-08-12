package com.example.test_platform.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseMigrationService {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateNewTaskColumns() {
        try {
            log.info("Checking database schema migrations for tasks table...");
            jdbcTemplate.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS has_detailed_answer BOOLEAN DEFAULT FALSE NOT NULL;");
            jdbcTemplate.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS task_variant VARCHAR(100);");
            jdbcTemplate.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS topic VARCHAR(300);");
            jdbcTemplate.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS subtopic VARCHAR(300);");
            jdbcTemplate.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS task_type VARCHAR(100);");
            log.info("Database schema migration completed successfully.");
        } catch (Exception e) {
            log.warn("Database schema migration notice (already applied or unsupported syntax): {}", e.getMessage());
        }
    }
}
