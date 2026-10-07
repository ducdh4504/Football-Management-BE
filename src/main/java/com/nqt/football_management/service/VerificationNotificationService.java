package com.nqt.football_management.service;

import java.time.Duration;

public interface VerificationNotificationService {
    void sendEmailVerificationOtp(String email, String otp, Duration ttl);
}
