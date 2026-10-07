package com.nqt.football_management.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "app.verification.email")
@Getter
@Setter
public class EmailVerificationProperties {
    private Duration otpTtl = Duration.ofMinutes(5);
    private Duration sessionTtl = Duration.ofHours(24);
    private Duration failedAttemptWindow = Duration.ofMinutes(15);
    private Duration resendCooldown = Duration.ofMinutes(1);
    private Duration resendWindow = Duration.ofHours(1);
    private Duration ipRateLimitWindow = Duration.ofMinutes(1);
    private int maxVerifyAttempts = 5;
    private int maxResendsPerWindow = 5;
    private int maxIpRequestsPerWindow = 20;
    private String otpPepper;
    private String mailFrom;
}
