package com.nqt.football_management.service.impl;

import com.nqt.football_management.config.EmailVerificationProperties;
import com.nqt.football_management.constants.ErrorCode;
import com.nqt.football_management.constants.UserStatus;
import com.nqt.football_management.entity.User;
import com.nqt.football_management.exception.GlobalException;
import com.nqt.football_management.model.response.EmailVerificationResponse;
import com.nqt.football_management.model.response.RegistrationResponse;
import com.nqt.football_management.repository.UserRepository;
import com.nqt.football_management.service.EmailOtpService;
import com.nqt.football_management.service.EmailVerificationService;
import com.nqt.football_management.service.UserRegistrationPersistenceService;
import com.nqt.football_management.service.VerificationNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {
    private static final String KEY_PREFIX = "identity:verification:email:";

    private static final DefaultRedisScript<Long> VERIFY_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then
                return -1
            end
            if redis.call('HGET', KEYS[1], 'consumed') == '1' then
                return -3
            end
            local attempts = tonumber(redis.call('GET', KEYS[2]) or '0')
            local maximum = tonumber(ARGV[2])
            if attempts >= maximum then
                return -2
            end
            if redis.call('HGET', KEYS[1], 'otpDigest') ~= ARGV[1] then
                attempts = redis.call('INCR', KEYS[2])
                if attempts == 1 then
                    redis.call('EXPIRE', KEYS[2], tonumber(ARGV[3]))
                end
                if attempts >= maximum then
                    return -2
                end
                return 0
            end
            redis.call('HSET', KEYS[1], 'consumed', '1')
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('HGET', KEYS[1], 'otpDigest') == ARGV[1]
                    and redis.call('HGET', KEYS[1], 'consumed') == '1' then
                redis.call('HSET', KEYS[1], 'consumed', '0')
                return 1
            end
            return 0
            """, Long.class);

    private static final DefaultRedisScript<Long> WINDOW_COUNTER_SCRIPT = new DefaultRedisScript<>("""
            local value = redis.call('INCR', KEYS[1])
            if value == 1 then
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
            end
            return value
            """, Long.class);

    private static final DefaultRedisScript<Long> RESEND_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('HGET', KEYS[1], 'consumed') == '1' then
                return -3
            end
            if not redis.call('SET', KEYS[2], '1', 'NX', 'EX', tonumber(ARGV[1])) then
                return -2
            end
            local count = redis.call('INCR', KEYS[3])
            if count == 1 then
                redis.call('EXPIRE', KEYS[3], tonumber(ARGV[2]))
            end
            if count > tonumber(ARGV[3]) then
                return -1
            end
            redis.call('HSET', KEYS[1],
                    'userId', ARGV[4],
                    'otpDigest', ARGV[5],
                    'consumed', '0')
            redis.call('EXPIRE', KEYS[1], tonumber(ARGV[6]))
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final EmailVerificationProperties properties;
    private final EmailOtpService emailOtpService;
    private final VerificationNotificationService notificationService;
    private final UserRegistrationPersistenceService registrationPersistenceService;
    private final UserRepository userRepository;

    @Override
    public RegistrationResponse createChallenge(User user) {
        String verificationId = UUID.randomUUID().toString();
        String otp = emailOtpService.generate();
        String digest = emailOtpService.digest(verificationId, user.getId(), otp);

        try {
            storeChallenge(verificationId, user.getId(), digest);
            redisTemplate.opsForValue().set(
                    sessionKey(verificationId),
                    user.getId().toString(),
                    properties.getSessionTtl()
            );
            redisTemplate.opsForValue().set(
                    cooldownKey(user.getId()),
                    "1",
                    properties.getResendCooldown()
            );
        } catch (DataAccessException exception) {
            cleanup(verificationId, user.getId());
            throw verificationUnavailable();
        }

        sendWithoutExposingSecret(user, verificationId, otp);
        return challengeResponse(verificationId, false);
    }

    @Override
    public EmailVerificationResponse verify(
            String verificationId,
            String otp,
            String clientAddress
    ) {
        enforceIpRateLimit("verify", clientAddress);
        UUID userId = resolveUserId(verificationId);
        User user = userRepository.findById(userId).orElseThrow(this::invalidVerification);

        if (user.getStatus() == UserStatus.ACTIVE) {
            cleanup(verificationId, userId);
            return EmailVerificationResponse.builder().verified(true).build();
        }
        requirePending(user);

        String digest = emailOtpService.digest(verificationId, userId, otp);
        Long result;
        try {
            result = redisTemplate.execute(
                    VERIFY_SCRIPT,
                    List.of(challengeKey(verificationId), failedKey(userId)),
                    digest,
                    Integer.toString(properties.getMaxVerifyAttempts()),
                    Long.toString(properties.getFailedAttemptWindow().toSeconds())
            );
        } catch (DataAccessException exception) {
            throw verificationUnavailable();
        }

        if (result == null || result == -1L || result == 0L || result == -3L) {
            throw invalidVerification();
        }
        if (result == -2L) {
            throw new GlobalException(
                    ErrorCode.TOO_MANY_REQUESTS,
                    "Too many verification attempts"
            );
        }

        try {
            registrationPersistenceService.activatePendingUser(userId);
        } catch (RuntimeException exception) {
            releaseChallenge(verificationId, digest);
            throw exception;
        }

        cleanup(verificationId, userId);
        return EmailVerificationResponse.builder().verified(true).build();
    }

    @Override
    public RegistrationResponse resend(String verificationId, String clientAddress) {
        enforceIpRateLimit("resend", clientAddress);
        UUID userId = resolveUserId(verificationId);
        User user = userRepository.findById(userId).orElseThrow(this::invalidVerification);

        if (user.getStatus() == UserStatus.ACTIVE) {
            cleanup(verificationId, userId);
            return RegistrationResponse.builder()
                    .verificationId(verificationId)
                    .emailVerified(true)
                    .build();
        }
        requirePending(user);

        String otp = emailOtpService.generate();
        String digest = emailOtpService.digest(verificationId, userId, otp);
        Long result;
        try {
            result = redisTemplate.execute(
                    RESEND_SCRIPT,
                    List.of(
                            challengeKey(verificationId),
                            cooldownKey(userId),
                            resendCountKey(userId)
                    ),
                    Long.toString(properties.getResendCooldown().toSeconds()),
                    Long.toString(properties.getResendWindow().toSeconds()),
                    Integer.toString(properties.getMaxResendsPerWindow()),
                    userId.toString(),
                    digest,
                    Long.toString(properties.getOtpTtl().toSeconds())
            );
        } catch (DataAccessException exception) {
            throw verificationUnavailable();
        }

        if (result == null || result == -1L) {
            throw new GlobalException(
                    ErrorCode.TOO_MANY_REQUESTS,
                    "Email verification resend limit exceeded"
            );
        }
        if (result == -2L) {
            throw new GlobalException(
                    ErrorCode.TOO_MANY_REQUESTS,
                    "Please wait before requesting another verification code"
            );
        }
        if (result == -3L) {
            throw new GlobalException(
                    ErrorCode.CONFLICT,
                    "Email verification is already in progress"
            );
        }

        try {
            redisTemplate.expire(sessionKey(verificationId), properties.getSessionTtl());
        } catch (DataAccessException exception) {
            throw verificationUnavailable();
        }

        sendWithoutExposingSecret(user, verificationId, otp);
        return challengeResponse(verificationId, false);
    }

    private void storeChallenge(String verificationId, UUID userId, String digest) {
        String key = challengeKey(verificationId);
        redisTemplate.opsForHash().putAll(key, Map.of(
                "userId", userId.toString(),
                "otpDigest", digest,
                "consumed", "0"
        ));
        redisTemplate.expire(key, properties.getOtpTtl());
    }

    private void enforceIpRateLimit(String action, String clientAddress) {
        String key = KEY_PREFIX + "rate:" + action + ":" + addressDigest(clientAddress);
        try {
            Long count = redisTemplate.execute(
                    WINDOW_COUNTER_SCRIPT,
                    List.of(key),
                    Long.toString(properties.getIpRateLimitWindow().toSeconds())
            );
            if (count == null || count > properties.getMaxIpRequestsPerWindow()) {
                throw new GlobalException(ErrorCode.TOO_MANY_REQUESTS);
            }
        } catch (DataAccessException exception) {
            throw verificationUnavailable();
        }
    }

    private UUID resolveUserId(String verificationId) {
        if (!isUuid(verificationId)) {
            throw invalidVerification();
        }
        try {
            String userId = redisTemplate.opsForValue().get(sessionKey(verificationId));
            return userId == null ? failInvalidVerification() : UUID.fromString(userId);
        } catch (IllegalArgumentException exception) {
            throw invalidVerification();
        } catch (DataAccessException exception) {
            throw verificationUnavailable();
        }
    }

    private UUID failInvalidVerification() {
        throw invalidVerification();
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException | NullPointerException exception) {
            return false;
        }
    }

    private void requirePending(User user) {
        if (user.getStatus() != UserStatus.PENDING_EMAIL_VERIFICATION) {
            throw new GlobalException(ErrorCode.CONFLICT, "Account cannot be verified");
        }
    }

    private void releaseChallenge(String verificationId, String digest) {
        try {
            redisTemplate.execute(
                    RELEASE_SCRIPT,
                    List.of(challengeKey(verificationId)),
                    digest
            );
        } catch (DataAccessException exception) {
            log.error("Could not release email verification challenge verificationId={}", verificationId);
        }
    }

    private void cleanup(String verificationId, UUID userId) {
        try {
            redisTemplate.delete(List.of(
                    challengeKey(verificationId),
                    sessionKey(verificationId),
                    failedKey(userId),
                    cooldownKey(userId),
                    resendCountKey(userId)
            ));
        } catch (DataAccessException exception) {
            log.warn("Could not clean up completed email verification verificationId={} userId={}",
                    verificationId, userId);
        }
    }

    private void sendWithoutExposingSecret(User user, String verificationId, String otp) {
        try {
            notificationService.sendEmailVerificationOtp(
                    user.getEmail(),
                    otp,
                    properties.getOtpTtl()
            );
        } catch (RuntimeException exception) {
            log.warn("Verification email delivery failed verificationId={} userId={} errorType={}",
                    verificationId, user.getId(), exception.getClass().getSimpleName());
        }
    }

    private String addressDigest(String clientAddress) {
        String value = clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress;
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private RegistrationResponse challengeResponse(String verificationId, boolean verified) {
        return RegistrationResponse.builder()
                .verificationId(verificationId)
                .expiresInSeconds(properties.getOtpTtl().toSeconds())
                .resendAfterSeconds(properties.getResendCooldown().toSeconds())
                .emailVerified(verified)
                .build();
    }

    private GlobalException invalidVerification() {
        return new GlobalException(
                ErrorCode.DATA_INVALID,
                "Verification code is invalid or expired"
        );
    }

    private GlobalException verificationUnavailable() {
        return new GlobalException(
                ErrorCode.SERVICE_UNAVAILABLE,
                "Email verification is temporarily unavailable"
        );
    }

    private String challengeKey(String verificationId) {
        return KEY_PREFIX + "challenge:" + verificationId;
    }

    private String sessionKey(String verificationId) {
        return KEY_PREFIX + "session:" + verificationId;
    }

    private String failedKey(UUID userId) {
        return KEY_PREFIX + "failed:" + userId;
    }

    private String cooldownKey(UUID userId) {
        return KEY_PREFIX + "resend-cooldown:" + userId;
    }

    private String resendCountKey(UUID userId) {
        return KEY_PREFIX + "resend-count:" + userId;
    }
}
