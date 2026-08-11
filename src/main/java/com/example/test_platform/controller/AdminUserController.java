package com.example.test_platform.controller;

import com.example.test_platform.dto.request.CreateUserRequest;
import com.example.test_platform.dto.request.UpdateSubscriptionRequest;
import com.example.test_platform.dto.response.CreateUserResponse;
import com.example.test_platform.dto.response.UserResponse;
import com.example.test_platform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponse> getTeachers() {
        return userService.getTeachersForAdminPanel();
    }

    @PostMapping
    public CreateUserResponse createTeacher(@Valid @RequestBody CreateUserRequest request) {
        return userService.createTeacher(request);
    }

    @PutMapping("/{id}")
    public UserResponse updateTeacher(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return userService.updateTeacher(id, request.getDisplayName(), request.getLogin());
    }

    @PutMapping("/{id}/subscription")
    public UserResponse updateSubscription(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubscriptionRequest request
    ) {
        return userService.updateSubscription(id, request);
    }

    @PostMapping("/{id}/reset-password")
    public CreateUserResponse resetPassword(@PathVariable Long id) {
        return userService.resetTeacherPassword(id);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public void deleteTeacher(@PathVariable Long id) {
        userService.deleteTeacher(id);
    }

    @lombok.Getter
    @lombok.Setter
    public static class UpdateUserRequest {
        private String displayName;
        private String login;
    }
}
