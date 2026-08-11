package com.example.test_platform.service;

import com.example.test_platform.dto.request.ChangePasswordRequest;
import com.example.test_platform.dto.request.LoginRequest;
import com.example.test_platform.dto.response.AuthResponse;
import com.example.test_platform.dto.response.UserResponse;
import com.example.test_platform.security.JwtService;
import com.example.test_platform.security.UserPrincipal;
import com.example.test_platform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        userService.updateLastLogin(principal.getLogin());

        UserPrincipal refreshedPrincipal = new UserPrincipal(userService.getByLogin(principal.getLogin()));
        String token = jwtService.generateToken(refreshedPrincipal);

        return AuthResponse.builder()
                .token(token)
                .user(UserResponse.from(userService.getByLogin(principal.getLogin())))
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return UserResponse.from(userService.getById(currentUser.getId()));
    }

    @Transactional
    public UserResponse changePassword(ChangePasswordRequest request) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        userService.changePassword(
                currentUser.getId(),
                request.getCurrentPassword(),
                request.getNewPassword()
        );
        return UserResponse.from(userService.getById(currentUser.getId()));
    }
}
