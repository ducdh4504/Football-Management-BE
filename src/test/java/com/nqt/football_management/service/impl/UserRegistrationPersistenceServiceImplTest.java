package com.nqt.football_management.service.impl;

import com.nqt.football_management.constants.Gender;
import com.nqt.football_management.constants.Role;
import com.nqt.football_management.constants.UserStatus;
import com.nqt.football_management.entity.User;
import com.nqt.football_management.mapper.UserMapper;
import com.nqt.football_management.model.request.RegisterRequest;
import com.nqt.football_management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationPersistenceServiceImplTest {
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserRegistrationPersistenceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserRegistrationPersistenceServiceImpl(
                userMapper,
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void registrationPersistsPendingUnverifiedClientWithoutIssuingToken() {
        RegisterRequest request = RegisterRequest.builder()
                .email("  Client@Example.com ")
                .phone("0912345678")
                .password("StrongPassword1!")
                .build();
        User mapped = User.builder().gender(Gender.UNKNOWN).build();

        when(userMapper.toEntity(request)).thenReturn(mapped);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(mapped)).thenReturn(mapped);

        User result = service.createPendingUser(request);

        assertThat(result.getEmail()).isEqualTo("client@example.com");
        assertThat(result.getRole()).isEqualTo(Role.CLIENT);
        assertThat(result.getStatus()).isEqualTo(UserStatus.PENDING_EMAIL_VERIFICATION);
        assertThat(result.isEmailVerified()).isFalse();
        assertThat(result.getEmailVerifiedAt()).isNull();
        assertThat(result.getPassword()).isEqualTo("encoded-password");
        verify(userRepository).saveAndFlush(mapped);
    }

    @Test
    void activationUpdatesAllPersistentVerificationFields() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .status(UserStatus.PENDING_EMAIL_VERIFICATION)
                .emailVerified(false)
                .build();
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = service.activatePendingUser(userId);

        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.isEmailVerified()).isTrue();
        assertThat(result.getEmailVerifiedAt()).isNotNull();
        verify(userRepository).save(user);
    }
}
