package com.nqt.football_management.service.impl;

import com.nqt.football_management.constants.ErrorCode;
import com.nqt.football_management.constants.Role;
import com.nqt.football_management.constants.UserStatus;
import com.nqt.football_management.entity.User;
import com.nqt.football_management.exception.GlobalException;
import com.nqt.football_management.mapper.UserMapper;
import com.nqt.football_management.model.request.RegisterRequest;
import com.nqt.football_management.repository.UserRepository;
import com.nqt.football_management.service.UserRegistrationPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationPersistenceServiceImpl implements UserRegistrationPersistenceService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User createPendingUser(RegisterRequest request) {
        String normalizedEmail = request.getEmail().strip().toLowerCase(Locale.ROOT);
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new GlobalException(ErrorCode.CONFLICT, "Phone is already in use");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new GlobalException(ErrorCode.CONFLICT, "Email is already in use");
        }

        User entity = userMapper.toEntity(request);
        entity.setEmail(normalizedEmail);
        entity.setRole(Role.CLIENT);
        entity.setStatus(UserStatus.PENDING_EMAIL_VERIFICATION);
        entity.setEmailVerified(false);
        entity.setEmailVerifiedAt(null);
        entity.setPassword(passwordEncoder.encode(request.getPassword()));

        try {
            return userRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            log.warn(exception.getMessage());
            throw new GlobalException(ErrorCode.CONFLICT, "Email or phone is already in use");
        }
    }

    @Override
    @Transactional
    public User activatePendingUser(UUID userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> invalidVerification());

        if (user.getStatus() == UserStatus.ACTIVE && user.isEmailVerified()) {
            return user;
        }
        if (user.getStatus() != UserStatus.PENDING_EMAIL_VERIFICATION) {
            throw new GlobalException(ErrorCode.CONFLICT, "Account cannot be verified");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void discardPendingUser(UUID userId) {
        userRepository.findByIdForUpdate(userId)
                .filter(user -> user.getStatus() == UserStatus.PENDING_EMAIL_VERIFICATION)
                .ifPresent(userRepository::delete);
    }

    private GlobalException invalidVerification() {
        return new GlobalException(
                ErrorCode.DATA_INVALID,
                "Verification code is invalid or expired"
        );
    }
}
