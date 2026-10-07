package com.nqt.football_management.service.impl;

import com.nqt.football_management.config.SupabaseProperties;
import com.nqt.football_management.constants.ErrorCode;
import com.nqt.football_management.exception.GlobalException;
import com.nqt.football_management.service.SupabaseAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class SupabaseAuthServiceImplTest {
    private MockRestServiceServer server;
    private SupabaseAuthService service;

    @BeforeEach
    void setUp() {
        SupabaseProperties properties = new SupabaseProperties();
        properties.setUrl("https://project.supabase.co");
        properties.setApiKey("test-api-key");

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new SupabaseAuthServiceImpl(properties, builder.build());
    }

    @Test
    void verifiesGoogleIdentityThroughSupabaseUserEndpoint() {
        server.expect(once(), requestTo("https://project.supabase.co/auth/v1/user"))
                .andExpect(header("apikey", "test-api-key"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer supabase-token"))
                .andRespond(withSuccess("""
                        {
                          "email": "Client@Example.com",
                          "email_confirmed_at": "2026-10-05T10:00:00Z",
                          "app_metadata": {"provider": "google", "providers": ["google"]},
                          "user_metadata": {
                            "full_name": "Client Name",
                            "avatar_url": "https://example.com/avatar.png"
                          },
                          "identities": [{"provider": "google"}]
                        }
                        """, MediaType.APPLICATION_JSON));

        SupabaseAuthService.GoogleIdentity identity =
                service.verifyGoogleAccessToken("supabase-token");

        assertThat(identity.email()).isEqualTo("Client@Example.com");
        assertThat(identity.fullName()).isEqualTo("Client Name");
        assertThat(identity.avatarUrl()).isEqualTo("https://example.com/avatar.png");
        server.verify();
    }

    @Test
    void rejectsTokenThatSupabaseDoesNotAuthenticate() {
        server.expect(requestTo("https://project.supabase.co/auth/v1/user"))
                .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> service.verifyGoogleAccessToken("invalid-token"))
                .isInstanceOfSatisfying(GlobalException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
        server.verify();
    }

    @Test
    void rejectsAuthenticatedSupabaseUserWithoutGoogleIdentity() {
        server.expect(requestTo("https://project.supabase.co/auth/v1/user"))
                .andRespond(withSuccess("""
                        {
                          "email": "client@example.com",
                          "email_confirmed_at": "2026-10-05T10:00:00Z",
                          "app_metadata": {"provider": "email", "providers": ["email"]},
                          "user_metadata": {},
                          "identities": [{"provider": "email"}]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.verifyGoogleAccessToken("email-token"))
                .isInstanceOfSatisfying(GlobalException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
        server.verify();
    }
}
