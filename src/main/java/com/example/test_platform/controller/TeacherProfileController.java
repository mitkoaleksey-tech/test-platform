package com.example.test_platform.controller;

import com.example.test_platform.dto.response.UserResponse;
import com.example.test_platform.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherProfileController {

    private final AuthService authService;

    @GetMapping("/profile")
    public UserResponse profile() {
        return authService.getCurrentUserProfile();
    }
}
