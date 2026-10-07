package com.nqt.football_management.service.impl;

import com.nqt.football_management.constants.Gender;
import com.nqt.football_management.constants.Role;
import com.nqt.football_management.constants.UserStatus;
import com.nqt.football_management.entity.User;
import com.nqt.football_management.mapper.UserMapper;
import com.nqt.football_management.model.request.GoogleLoginRequest;
import com.nqt.football_management.model.response.UserResponse;
import com.nqt.football_management.repository.UserRepository;
import com.nqt.football_management.service.EmailVerificationService;
import com.nqt.football_management.service.SupabaseAuthService;
import com.nqt.football_management.service.UserRegistrationPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationConfiguration authenticationConfiguration;
    @Mock
    private TokenServiceImpl tokenService;
    @Mock
    private UserRegistrationPersistenceService registrationPersistenceService;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private SupabaseAuthService supabaseAuthService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthenticationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthenticationServiceImpl(
                userMapper,
                userRepository,
                authenticationConfiguration,
                tokenService,
                registrationPersistenceService,
                emailVerificationService,
                supabaseAuthService,
                passwordEncoder
        );
    }

    @Test
    void googleLoginCreatesVerifiedClientAndReturnsApplicationToken() {
        GoogleLoginRequest request = GoogleLoginRequest.builder()
                .accessToken("supabase-token")
                .build();
        SupabaseAuthService.GoogleIdentity identity = new SupabaseAuthService.GoogleIdentity(
                " Client@Example.com ",
                "Client Name",
                "https://example.com/avatar.png"
        );
        UserResponse mappedResponse = new UserResponse();

        when(supabaseAuthService.verifyGoogleAccessToken("supabase-token")).thenReturn(identity);
        when(userRepository.findByEmailIgnoreCase("client@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-random-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(mappedResponse);
        when(tokenService.generateAccessToken(any(User.class))).thenReturn("application-token");

        UserResponse response = service.loginWithGoogle(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("client@example.com");
        assertThat(saved.getFullName()).isEqualTo("Client Name");
        assertThat(saved.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(saved.getGender()).isEqualTo(Gender.UNKNOWN);
        assertThat(saved.getRole()).isEqualTo(Role.CLIENT);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.isEmailVerified()).isTrue();
        assertThat(saved.getEmailVerifiedAt()).isNotNull();
        assertThat(saved.getPassword()).isEqualTo("encoded-random-password");
        assertThat(response.getAccessToken()).isEqualTo("application-token");
    }
}
