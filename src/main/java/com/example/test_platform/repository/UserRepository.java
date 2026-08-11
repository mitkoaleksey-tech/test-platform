package com.example.test_platform.repository;

import com.example.test_platform.domain.entity.User;
import com.example.test_platform.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLogin(String login);

    boolean existsByLogin(String login);

    List<User> findByRoleOrderByDisplayNameAsc(UserRole role);
}
