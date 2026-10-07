package com.nqt.football_management.service.impl;

import com.nqt.football_management.config.EmailVerificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmailOtpServiceImplTest {
    private EmailOtpServiceImpl service;

    @BeforeEach
    void setUp() {
        EmailVerificationProperties properties = new EmailVerificationProperties();
        properties.setOtpPepper("test-only-otp-pepper-with-enough-entropy");
        service = new EmailOtpServiceImpl(properties);
    }

    @Test
    void generatedOtpAlwaysContainsSixDigits() {
        for (int index = 0; index < 100; index++) {
            assertThat(service.generate()).matches("^[0-9]{6}$");
        }
    }

    @Test
    void digestIsStableAndBoundToChallengeAndUser() {
        UUID userId = UUID.randomUUID();
        String digest = service.digest("verification-one", userId, "004271");

        assertThat(digest)
                .hasSize(64)
                .isEqualTo(service.digest("verification-one", userId, "004271"))
                .isNotEqualTo(service.digest("verification-two", userId, "004271"))
                .isNotEqualTo("004271");
    }
}
