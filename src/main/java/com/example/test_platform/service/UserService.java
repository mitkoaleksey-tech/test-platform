package com.example.test_platform.service;

import com.example.test_platform.domain.entity.User;
import com.example.test_platform.domain.enums.UserRole;
import com.example.test_platform.dto.request.CreateUserRequest;
import com.example.test_platform.dto.request.UpdateSubscriptionRequest;
import com.example.test_platform.dto.response.CreateUserResponse;
import com.example.test_platform.dto.response.UserResponse;
import com.example.test_platform.exception.ApiException;
import com.example.test_platform.repository.UserRepository;
import com.example.test_platform.util.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.example.test_platform.repository.TestVariantRepository testVariantRepository;
    private final com.example.test_platform.repository.StudentRepository studentRepository;
    private final com.example.test_platform.repository.TestAttemptRepository testAttemptRepository;
    private final com.example.test_platform.repository.StudentRetakePermissionRepository studentRetakePermissionRepository;
    private final com.example.test_platform.repository.TestVariantTaskRepository testVariantTaskRepository;
    private final com.example.test_platform.repository.StudentAnswerRepository studentAnswerRepository;

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional(readOnly = true)
    public User getByLogin(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getTeachersForAdminPanel() {
        return userRepository.findByRoleOrderByDisplayNameAsc(UserRole.TEACHER).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public CreateUserResponse createTeacher(CreateUserRequest request) {
        if (userRepository.existsByLogin(request.getLogin())) {
            throw new ApiException(HttpStatus.CONFLICT, "Login already exists");
        }

        String temporaryPassword = PasswordGenerator.generateTemporaryPassword(10);

        User user = new User();
        user.setLogin(request.getLogin());
        user.setDisplayName(request.getDisplayName());
        user.setRole(UserRole.TEACHER);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setTemporaryPassword(true);
        user.setRawTemporaryPassword(temporaryPassword);
        user.setNextPaymentAt(request.getNextPaymentAt());

        User savedUser = userRepository.save(user);

        return CreateUserResponse.builder()
                .user(UserResponse.from(savedUser))
                .temporaryPassword(temporaryPassword)
                .build();
    }

    @Transactional
    public UserResponse updateTeacher(Long userId, String displayName, String login) {
        User user = getById(userId);
        if (user.getRole() != UserRole.TEACHER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only teacher accounts can be edited");
        }
        if (login != null && !login.isBlank() && !login.equals(user.getLogin())) {
            if (userRepository.existsByLogin(login.trim())) {
                throw new ApiException(HttpStatus.CONFLICT, "Логин уже занят");
            }
            user.setLogin(login.trim());
        }
        if (displayName != null && !displayName.isBlank()) {
            user.setDisplayName(displayName.trim());
        }
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateSubscription(Long userId, UpdateSubscriptionRequest request) {
        User user = getById(userId);

        if (user.getRole() != UserRole.TEACHER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Subscription can be updated only for teachers");
        }

        user.setSubscriptionPaidAt(request.getSubscriptionPaidAt());
        user.setNextPaymentAt(request.getNextPaymentAt());

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void updateLastLogin(String login) {
        User user = getByLogin(login);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setTemporaryPassword(false);
        user.setRawTemporaryPassword(null);
        userRepository.save(user);
    }

    @Transactional
    public CreateUserResponse resetTeacherPassword(Long userId) {
        User user = getById(userId);
        if (user.getRole() != UserRole.TEACHER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password can be reset only for teachers");
        }

        String temporaryPassword = PasswordGenerator.generateTemporaryPassword(10);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setTemporaryPassword(true);
        user.setRawTemporaryPassword(temporaryPassword);

        User updatedUser = userRepository.save(user);

        return CreateUserResponse.builder()
                .user(UserResponse.from(updatedUser))
                .temporaryPassword(temporaryPassword)
                .build();
    }

    @Transactional
    public void deleteTeacher(Long userId) {
        User user = getById(userId);
        if (user.getRole() != UserRole.TEACHER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only teachers can be deleted");
        }

        // Delete all variants created by teacher
        List<com.example.test_platform.domain.entity.TestVariant> variants = testVariantRepository.findByTeacherIdOrderByCreatedAtDesc(userId);
        for (com.example.test_platform.domain.entity.TestVariant v : variants) {
            studentRetakePermissionRepository.deleteByTestVariantId(v.getId());
            List<com.example.test_platform.domain.entity.TestAttempt> attempts = testAttemptRepository.findByTestVariantIdOrderByStartedAtDesc(v.getId());
            for (com.example.test_platform.domain.entity.TestAttempt a : attempts) {
                studentAnswerRepository.deleteByAttemptId(a.getId());
            }
            testAttemptRepository.deleteByTestVariantId(v.getId());
            testVariantTaskRepository.deleteByTestVariantId(v.getId());
            testVariantRepository.delete(v);
        }

        // Delete all students of teacher
        List<com.example.test_platform.domain.entity.Student> students = studentRepository.findByTeacherIdOrderByDisplayNameAsc(userId);
        for (com.example.test_platform.domain.entity.Student s : students) {
            studentRetakePermissionRepository.deleteByStudentId(s.getId());
            List<com.example.test_platform.domain.entity.TestAttempt> attempts = testAttemptRepository.findByStudentIdOrderByStartedAtDesc(s.getId());
            for (com.example.test_platform.domain.entity.TestAttempt a : attempts) {
                studentAnswerRepository.deleteByAttemptId(a.getId());
            }
            testAttemptRepository.deleteByStudentId(s.getId());
            studentRepository.delete(s);
        }

        userRepository.delete(user);
    }
}
