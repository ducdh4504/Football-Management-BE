package com.nqt.football_management.service.impl;

import com.nqt.football_management.config.EmailVerificationProperties;
import com.nqt.football_management.constants.ErrorCode;
import com.nqt.football_management.constants.UserStatus;
import com.nqt.football_management.entity.User;
import com.nqt.football_management.exception.GlobalException;
import com.nqt.football_management.model.response.EmailVerificationResponse;
import com.nqt.football_management.repository.UserRepository;
import com.nqt.football_management.service.EmailOtpService;
import com.nqt.football_management.service.UserRegistrationPersistenceService;
import com.nqt.football_management.service.VerificationNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private EmailOtpService emailOtpService;
    @Mock
    private VerificationNotificationService notificationService;
    @Mock
    private UserRegistrationPersistenceService registrationPersistenceService;
    @Mock
    private UserRepository userRepository;

    private EmailVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new EmailVerificationServiceImpl(
                redisTemplate,
                new EmailVerificationProperties(),
                emailOtpService,
                notificationService,
                registrationPersistenceService,
                userRepository
        );
    }

    @Test
    void validOtpActivatesPendingAccount() {
        String verificationId = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();
        User pendingUser = User.builder()
                .id(userId)
                .status(UserStatus.PENDING_EMAIL_VERIFICATION)
                .build();

        when(valueOperations.get("identity:verification:email:session:" + verificationId))
                .thenReturn(userId.toString());
        when(userRepository.findById(userId)).thenReturn(Optional.of(pendingUser));
        when(emailOtpService.digest(verificationId, userId, "004271")).thenReturn("digest");
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L, 1L);
        when(registrationPersistenceService.activatePendingUser(userId)).thenReturn(pendingUser);

        EmailVerificationResponse response = service.verify(
                verificationId,
                "004271",
                "127.0.0.1"
        );

        assertThat(response.isVerified()).isTrue();
        verify(registrationPersistenceService).activatePendingUser(userId);
    }

    @Test
    void invalidOtpDoesNotActivateAccount() {
        String verificationId = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();
        User pendingUser = User.builder()
                .id(userId)
                .status(UserStatus.PENDING_EMAIL_VERIFICATION)
                .build();

        when(valueOperations.get("identity:verification:email:session:" + verificationId))
                .thenReturn(userId.toString());
        when(userRepository.findById(userId)).thenReturn(Optional.of(pendingUser));
        when(emailOtpService.digest(verificationId, userId, "111111")).thenReturn("wrong-digest");
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L, 0L);

        assertThatThrownBy(() -> service.verify(verificationId, "111111", "127.0.0.1"))
                .isInstanceOfSatisfying(GlobalException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DATA_INVALID));
        verify(registrationPersistenceService, never()).activatePendingUser(userId);
    }
}
