package com.nqt.football_management.controller;


import com.nqt.football_management.model.request.GoogleLoginRequest;
import com.nqt.football_management.model.request.LoginRequest;
import com.nqt.football_management.model.request.RegisterRequest;
import com.nqt.football_management.model.request.ResendEmailVerificationRequest;
import com.nqt.football_management.model.request.VerifyEmailRequest;
import com.nqt.football_management.model.response.APIResponse;
import com.nqt.football_management.model.response.EmailVerificationResponse;
import com.nqt.football_management.model.response.RegistrationResponse;
import com.nqt.football_management.model.response.UserResponse;
import com.nqt.football_management.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(
        name = "Authentication",
        description = "Public APIs for client registration and credential-based login"
)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {
    AuthenticationService authenticationService;

    @Operation(
            summary = "Register a client account",
            description = "Creates a pending CLIENT account and sends a six-digit email verification code"
    )
    @PostMapping("register")
    public ResponseEntity<APIResponse<RegistrationResponse>> register(
            @RequestBody @Valid RegisterRequest request
    ) {
        RegistrationResponse response = authenticationService.register(request);
        return ResponseEntity.status(202)
                .body(APIResponse.response(
                        202,
                        "Registration accepted. Complete email verification to activate the account.",
                        response,
                        true
                ));
    }

    @Operation(
            summary = "Verify registration email",
            description = "Activates a pending account when the submitted verification code is valid"
    )
    @PostMapping("email-verification/verify")
    public ResponseEntity<APIResponse<EmailVerificationResponse>> verifyEmail(
            @RequestBody @Valid VerifyEmailRequest request,
            HttpServletRequest httpRequest
    ) {
        EmailVerificationResponse response = authenticationService.verifyEmail(
                request,
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Email verified successfully",
                response,
                true
        ));
    }

    @Operation(
            summary = "Resend registration verification code",
            description = "Invalidates the previous code and sends a new code when resend limits allow it"
    )
    @PostMapping("email-verification/resend")
    public ResponseEntity<APIResponse<RegistrationResponse>> resendEmailVerification(
            @RequestBody @Valid ResendEmailVerificationRequest request,
            HttpServletRequest httpRequest
    ) {
        RegistrationResponse response = authenticationService.resendEmailVerification(
                request,
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.status(202)
                .body(APIResponse.response(202, "Verification request accepted", response, true));
    }

    @Operation(
            summary = "Log in",
            description = "Authenticates an account by email and password, then returns an access token"
    )
    @PostMapping("login")
    public ResponseEntity<APIResponse<UserResponse>> login(@RequestBody @Valid LoginRequest request) {
        UserResponse response = authenticationService.login(request);
        return ResponseEntity.ok(APIResponse.response(200, "login successfully", response, true));
    }

    @Operation(
            summary = "Log in with Google",
            description = "Exchanges a Google-backed Supabase access token for an application access token"
    )
    @PostMapping("google")
    public ResponseEntity<APIResponse<UserResponse>> loginWithGoogle(
            @RequestBody @Valid GoogleLoginRequest request
    ) {
        UserResponse response = authenticationService.loginWithGoogle(request);
        return ResponseEntity.ok(APIResponse.response(200, "login successfully", response, true));
    }
}
