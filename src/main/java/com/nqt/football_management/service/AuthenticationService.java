package com.nqt.football_management.service;


import com.nqt.football_management.model.request.GoogleLoginRequest;
import com.nqt.football_management.model.request.LoginRequest;
import com.nqt.football_management.model.request.RegisterRequest;
import com.nqt.football_management.model.request.ResendEmailVerificationRequest;
import com.nqt.football_management.model.request.VerifyEmailRequest;
import com.nqt.football_management.model.response.EmailVerificationResponse;
import com.nqt.football_management.model.response.RegistrationResponse;
import com.nqt.football_management.model.response.UserResponse;
import jakarta.validation.Valid;

public interface AuthenticationService {
    RegistrationResponse register(@Valid RegisterRequest request);
    UserResponse login(@Valid LoginRequest request);
    UserResponse loginWithGoogle(@Valid GoogleLoginRequest request);
    EmailVerificationResponse verifyEmail(@Valid VerifyEmailRequest request, String clientAddress);
    RegistrationResponse resendEmailVerification(
            @Valid ResendEmailVerificationRequest request,
            String clientAddress
    );
}
