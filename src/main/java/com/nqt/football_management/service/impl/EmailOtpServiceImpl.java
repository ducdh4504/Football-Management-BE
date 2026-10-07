package com.nqt.football_management.service.impl;

import com.nqt.football_management.config.EmailVerificationProperties;
import com.nqt.football_management.constants.ErrorCode;
import com.nqt.football_management.exception.GlobalException;
import com.nqt.football_management.service.EmailOtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailOtpServiceImpl implements EmailOtpService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationProperties properties;

    @Override
    public String generate() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    @Override
    public String digest(String verificationId, UUID userId, String otp) {
        String pepper = properties.getOtpPepper();
        if (pepper == null
                || pepper.isBlank()
                || pepper.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new GlobalException(
                    ErrorCode.SERVICE_UNAVAILABLE,
                    "Email verification is temporarily unavailable"
            );
        }

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] value = (verificationId + ":" + userId + ":" + otp)
                    .getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(mac.doFinal(value));
        } catch (GeneralSecurityException exception) {
            throw new GlobalException(
                    ErrorCode.SERVICE_UNAVAILABLE,
                    "Email verification is temporarily unavailable"
            );
        }
    }
}
