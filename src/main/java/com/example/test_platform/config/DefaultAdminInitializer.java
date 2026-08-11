package com.example.test_platform.config;

import com.example.test_platform.domain.entity.User;
import com.example.test_platform.domain.enums.UserRole;
import com.example.test_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.default-admin.enabled:true}")
    private boolean enabled;

    @Value("${app.security.default-admin.login:admin}")
    private String login;

    @Value("${app.security.default-admin.password:admin123}")
    private String password;

    @Value("${app.security.default-admin.display-name:Администратор}")
    private String displayName;

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        String cleanDisplayName = displayName;
        if (cleanDisplayName == null || cleanDisplayName.contains("Ð") || cleanDisplayName.isBlank()) {
            cleanDisplayName = "Администратор";
        }

        var admins = userRepository.findByRoleOrderByDisplayNameAsc(UserRole.ADMIN);
        if (admins.isEmpty()) {
            User admin = new User();
            admin.setLogin(login);
            admin.setDisplayName(cleanDisplayName);
            admin.setRole(UserRole.ADMIN);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setTemporaryPassword(false);
            userRepository.save(admin);
            log.info("Default admin user created with login '{}'", login);
        } else {
            for (User admin : admins) {
                if (admin.getDisplayName() != null && admin.getDisplayName().contains("Ð")) {
                    admin.setDisplayName("Администратор");
                    userRepository.save(admin);
                }
            }
        }
    }
}
