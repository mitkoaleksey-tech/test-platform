package com.example.test_platform.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateUserResponse {

    private final UserResponse user;
    private final String temporaryPassword;
}
