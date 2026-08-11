package com.example.test_platform.dto.response;

import com.example.test_platform.domain.entity.User;
import com.example.test_platform.domain.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private final Long id;
    private final String login;
    private final String displayName;
    private final UserRole role;
    private final boolean temporaryPassword;
    private final String temporaryPasswordStr;
    private final int testsCreatedCount;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime subscriptionPaidAt;
    private final LocalDateTime nextPaymentAt;
    private final LocalDateTime createdAt;

    public static UserResponse from(User user) {
        String name = user.getDisplayName();
        if (name != null && name.contains("Ð")) {
            name = user.getRole() == UserRole.ADMIN ? "Администратор" : name;
        }
        return UserResponse.builder()
                .id(user.getId())
                .login(user.getLogin())
                .displayName(name)
                .role(user.getRole())
                .temporaryPassword(user.isTemporaryPassword())
                .temporaryPasswordStr(user.isTemporaryPassword() ? user.getRawTemporaryPassword() : null)
                .testsCreatedCount(user.getTestsCreatedCount())
                .lastLoginAt(user.getLastLoginAt())
                .subscriptionPaidAt(user.getSubscriptionPaidAt())
                .nextPaymentAt(user.getNextPaymentAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
