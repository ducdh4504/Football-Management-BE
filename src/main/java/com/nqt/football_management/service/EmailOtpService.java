package com.nqt.football_management.service;

import java.util.UUID;

public interface EmailOtpService {
    String generate();

    String digest(String verificationId, UUID userId, String otp);
}
