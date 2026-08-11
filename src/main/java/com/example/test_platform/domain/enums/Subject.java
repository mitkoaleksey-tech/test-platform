package com.example.test_platform.domain.enums;

public enum Subject {
    MATHEMATICS("Математика"),
    MATHEMATICS_BASE("Математика (Базовый уровень)"),
    MATHEMATICS_PROF("Математика (Профильный уровень)"),
    RUSSIAN("Русский язык"),
    PHYSICS("Физика"),
    INFORMATICS("Информатика"),
    SOCIAL_STUDIES("Обществознание"),
    HISTORY("История"),
    BIOLOGY("Биология"),
    CHEMISTRY("Химия"),
    GEOGRAPHY("География"),
    LITERATURE("Литература"),
    ENGLISH("Английский язык");

    private final String displayName;

    Subject(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
