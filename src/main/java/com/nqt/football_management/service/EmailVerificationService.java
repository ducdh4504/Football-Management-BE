package com.nqt.football_management.service;

import com.nqt.football_management.entity.User;
import com.nqt.football_management.model.response.EmailVerificationResponse;
import com.nqt.football_management.model.response.RegistrationResponse;

public interface EmailVerificationService {
    RegistrationResponse createChallenge(User user);

    EmailVerificationResponse verify(String verificationId, String otp, String clientAddress);

    RegistrationResponse resend(String verificationId, String clientAddress);
}
