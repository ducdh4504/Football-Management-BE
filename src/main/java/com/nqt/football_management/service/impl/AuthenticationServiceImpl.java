package com.nqt.football_management.service.impl;

import com.nqt.football_management.constants.ErrorCode;
import com.nqt.football_management.constants.Gender;
import com.nqt.football_management.constants.Role;
import com.nqt.football_management.constants.UserStatus;
import com.nqt.football_management.entity.User;
import com.nqt.football_management.exception.GlobalException;
import com.nqt.football_management.mapper.UserMapper;
import com.nqt.football_management.model.request.GoogleLoginRequest;
import com.nqt.football_management.model.request.LoginRequest;
import com.nqt.football_management.model.request.RegisterRequest;
import com.nqt.football_management.model.request.ResendEmailVerificationRequest;
import com.nqt.football_management.model.request.VerifyEmailRequest;
import com.nqt.football_management.model.response.EmailVerificationResponse;
import com.nqt.football_management.model.response.RegistrationResponse;
import com.nqt.football_management.model.response.UserResponse;
import com.nqt.football_management.repository.UserRepository;
import com.nqt.football_management.service.AuthenticationService;
import com.nqt.football_management.service.EmailVerificationService;
import com.nqt.football_management.service.SupabaseAuthService;
import com.nqt.football_management.service.UserRegistrationPersistenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationServiceImpl implements UserDetailsService, AuthenticationService {
    UserMapper userMapper;
    UserRepository userRepository;
    AuthenticationConfiguration authenticationConfiguration;
    TokenServiceImpl tokenService;
    UserRegistrationPersistenceService registrationPersistenceService;
    EmailVerificationService emailVerificationService;
    SupabaseAuthService supabaseAuthService;
    PasswordEncoder passwordEncoder;

    @Override
    public UserResponse login(@Valid LoginRequest request) {
        try {

            Authentication authentication =
                    authenticationConfiguration.getAuthenticationManager().authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    request.getEmail().strip().toLowerCase(Locale.ROOT),
                                    request.getPassword()
                            )
                    );

            User user = (User) authentication.getPrincipal();

            String accessToken =
                    tokenService.generateAccessToken(user);

            UserResponse response =
                    userMapper.toResponse(user);

            response.setAccessToken(accessToken);

            return response;

        } catch (LockedException e) {

            throw new GlobalException(ErrorCode.CONFLICT,"Account is banned");

        } catch (DisabledException e) {

            throw new GlobalException(ErrorCode.CONFLICT,"Account is inactive or awaiting email verification");

        } catch (BadCredentialsException e) {

            throw new GlobalException(ErrorCode.CONFLICT,
                    "Invalid email or password"
            );
        }
    }

    @Override
    @Transactional
    public UserResponse loginWithGoogle(@Valid GoogleLoginRequest request) {
        SupabaseAuthService.GoogleIdentity identity =
                supabaseAuthService.verifyGoogleAccessToken(request.getAccessToken());
        String email = identity.email().strip().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailIgnoreCase(email)
                .map(this::activateOrRejectGoogleUser)
                .orElseGet(() -> createGoogleUser(identity, email));

        UserResponse response = userMapper.toResponse(user);
        response.setAccessToken(tokenService.generateAccessToken(user));
        return response;
    }

    private User activateOrRejectGoogleUser(User user) {
        if (user.getStatus() == UserStatus.BANNED) {
            throw new GlobalException(ErrorCode.CONFLICT, "Account is banned");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new GlobalException(ErrorCode.CONFLICT, "Account is inactive");
        }
        if (user.getStatus() == UserStatus.PENDING_EMAIL_VERIFICATION || !user.isEmailVerified()) {
            user.setStatus(UserStatus.ACTIVE);
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
            return userRepository.save(user);
        }
        return user;
    }

    private User createGoogleUser(SupabaseAuthService.GoogleIdentity identity, String email) {
        if (email.length() > 255 || !email.contains("@")) {
            throw new GlobalException(ErrorCode.INVALID_TOKEN, "Invalid Google login token");
        }
        String fullName = identity.fullName();
        if (fullName == null || fullName.isBlank()) {
            fullName = email.substring(0, email.indexOf('@'));
        }
        if (fullName.length() > 255) {
            fullName = fullName.substring(0, 255);
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .fullName(fullName)
                .avatarUrl(identity.avatarUrl())
                .gender(Gender.UNKNOWN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .role(Role.CLIENT)
                .build();
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new GlobalException(ErrorCode.CONFLICT, "Email is already in use");
        }
    }

    @Override
    public RegistrationResponse register(@Valid RegisterRequest request) {
        User user = registrationPersistenceService.createPendingUser(request);
        try {
            return emailVerificationService.createChallenge(user);
        } catch (RuntimeException exception) {
            try {
                registrationPersistenceService.discardPendingUser(user.getId());
            } catch (RuntimeException cleanupException) {
                log.error("Could not discard pending account after challenge creation failed userId={}",
                        user.getId());
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @Override
    public EmailVerificationResponse verifyEmail(
            @Valid VerifyEmailRequest request,
            String clientAddress
    ) {
        return emailVerificationService.verify(
                request.getVerificationId(),
                request.getOtp(),
                clientAddress
        );
    }

    @Override
    public RegistrationResponse resendEmailVerification(
            @Valid ResendEmailVerificationRequest request,
            String clientAddress
    ) {
        return emailVerificationService.resend(request.getVerificationId(), clientAddress);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }


}
