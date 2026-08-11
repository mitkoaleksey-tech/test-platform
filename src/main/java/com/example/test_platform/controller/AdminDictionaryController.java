package com.example.test_platform.controller;

import com.example.test_platform.domain.entity.KimTaskSetting;
import com.example.test_platform.domain.entity.SystemExamType;
import com.example.test_platform.domain.entity.SystemSubject;
import com.example.test_platform.domain.entity.SystemTaskBank;
import com.example.test_platform.service.DictionaryService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dictionaries")
@RequiredArgsConstructor
public class AdminDictionaryController {

    private final DictionaryService dictionaryService;

    @GetMapping("/subjects")
    public List<SystemSubject> getSubjects() {
        try {
            List<SystemSubject> res = dictionaryService.getSubjects();
            return res != null ? res : List.of();
        } catch (Exception ex) {
            return List.of();
        }
    }

    @PostMapping("/subjects")
    @PreAuthorize("hasRole('ADMIN')")
    public SystemSubject addSubject(@RequestBody DictRequest request) {
        if (request == null || request.getDisplayName() == null || request.getDisplayName().isBlank()) {
            throw new com.example.test_platform.exception.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Укажите название предмета");
        }
        return dictionaryService.addSubject(request.getName(), request.getDisplayName(), request.getTotalTasks());
    }

    @PutMapping("/subjects/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SystemSubject updateSubject(@PathVariable Long id, @RequestBody DictRequest request) {
        return dictionaryService.updateSubject(id, request.getDisplayName(), request.getTotalTasks());
    }

    @DeleteMapping("/subjects/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteSubject(@PathVariable Long id) {
        dictionaryService.deleteSubject(id);
    }

    @GetMapping("/exams")
    public List<SystemExamType> getExamTypes() {
        try {
            List<SystemExamType> res = dictionaryService.getExamTypes();
            return res != null ? res : List.of();
        } catch (Exception ex) {
            return List.of();
        }
    }

    @PostMapping("/exams")
    public SystemExamType addExamType(@RequestBody DictRequest request) {
        return dictionaryService.addExamType(request.getName(), request.getDisplayName());
    }

    @org.springframework.web.bind.annotation.PutMapping("/exams/{id}")
    public SystemExamType updateExamType(@org.springframework.web.bind.annotation.PathVariable Long id, @RequestBody DictRequest request) {
        return dictionaryService.updateExamType(id, request.getDisplayName());
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/exams/{id}")
    public void deleteExamType(@org.springframework.web.bind.annotation.PathVariable Long id) {
        dictionaryService.deleteExamType(id);
    }

    @GetMapping("/banks")
    public List<SystemTaskBank> getTaskBanks() {
        try {
            List<SystemTaskBank> res = dictionaryService.getTaskBanks();
            return res != null ? res : List.of();
        } catch (Exception ex) {
            return List.of();
        }
    }

    @PostMapping("/banks")
    public SystemTaskBank addTaskBank(@RequestBody DictRequest request) {
        return dictionaryService.addTaskBank(request.getName(), request.getDisplayName());
    }

    @org.springframework.web.bind.annotation.PutMapping("/banks/{id}")
    public SystemTaskBank updateTaskBank(@org.springframework.web.bind.annotation.PathVariable Long id, @RequestBody DictRequest request) {
        return dictionaryService.updateTaskBank(id, request.getDisplayName());
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/banks/{id}")
    public void deleteTaskBank(@org.springframework.web.bind.annotation.PathVariable Long id) {
        dictionaryService.deleteTaskBank(id);
    }

    @GetMapping("/kim-settings")
    public List<KimTaskSetting> getKimSettings(
            @RequestParam String subject,
            @RequestParam String exam
    ) {
        try {
            List<KimTaskSetting> res = dictionaryService.getKimSettings(subject, exam);
            return res != null ? res : List.of();
        } catch (Exception ex) {
            return List.of();
        }
    }

    @PostMapping("/kim-settings")
    public KimTaskSetting setKimSetting(@RequestBody KimSettingRequest request) {
        return dictionaryService.setTaskMaxScore(request.getSubject(), request.getExam(), request.getTaskNumber(), request.getMaxScore());
    }

    @Getter
    @Setter
    public static class DictRequest {
        private String name;
        private String displayName;
        private Integer totalTasks;
    }

    @Getter
    @Setter
    public static class KimSettingRequest {
        private String subject;
        private String exam;
        private Integer taskNumber;
        private Integer maxScore;
    }
}
