package com.example.test_platform.controller;

import com.example.test_platform.dto.request.ChangePasswordRequest;
import com.example.test_platform.dto.request.LoginRequest;
import com.example.test_platform.dto.response.AuthResponse;
import com.example.test_platform.dto.response.UserResponse;
import com.example.test_platform.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me() {
        return authService.getCurrentUserProfile();
    }

    @PostMapping("/change-password")
    public UserResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(request);
    }
}
