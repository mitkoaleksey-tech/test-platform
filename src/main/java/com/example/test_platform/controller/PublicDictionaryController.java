package com.example.test_platform.controller;

import com.example.test_platform.domain.entity.SystemExamType;
import com.example.test_platform.domain.entity.SystemSubject;
import com.example.test_platform.domain.entity.SystemTaskBank;
import com.example.test_platform.service.DictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/dictionaries")
@RequiredArgsConstructor
public class PublicDictionaryController {

    private final DictionaryService dictionaryService;

    @GetMapping("/subjects")
    public List<SystemSubject> getSubjects() {
        return dictionaryService.getSubjects();
    }

    @GetMapping("/exams")
    public List<SystemExamType> getExamTypes() {
        return dictionaryService.getExamTypes();
    }

    @GetMapping("/banks")
    public List<SystemTaskBank> getTaskBanks() {
        return dictionaryService.getTaskBanks();
    }
}
