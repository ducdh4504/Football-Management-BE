package com.nqt.football_management.service.impl;

import com.nqt.football_management.config.AdminSeedProperties;
import com.nqt.football_management.constants.ErrorCode;
import com.nqt.football_management.constants.Gender;
import com.nqt.football_management.constants.Role;
import com.nqt.football_management.constants.UserStatus;
import com.nqt.football_management.entity.User;
import com.nqt.football_management.exception.GlobalException;
import com.nqt.football_management.repository.UserRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminDataSeederImpl implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties properties;
    private final Validator validator;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        // Validate only when bootstrapping: existing admins need no seed credentials.
        if (!validator.validate(properties).isEmpty()
                || properties.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new GlobalException(ErrorCode.DATA_INVALID,
                    "No ADMIN exists. Configure valid ADMIN_EMAIL, ADMIN_PASSWORD (8-72 characters, at most 72 UTF-8 bytes) and ADMIN_FULL_NAME");
        }
        if (userRepository.existsByEmail(properties.getEmail())) {
            throw new GlobalException(ErrorCode.CONFLICT,
                    "ADMIN_EMAIL is already assigned to another account; choose a different seed email");
        }

        User admin = User.builder()
                .email(properties.getEmail())
                .password(passwordEncoder.encode(properties.getPassword()))
                .fullName(properties.getFullName().strip())
                .gender(Gender.UNKNOWN)
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .build();
        userRepository.saveAndFlush(admin);
        log.info("Initial ADMIN account created");
    }
}
