package com.nqt.football_management.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nqt.football_management.config.SupabaseProperties;
import com.nqt.football_management.constants.ErrorCode;
import com.nqt.football_management.exception.GlobalException;
import com.nqt.football_management.service.SupabaseAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SupabaseAuthServiceImpl implements SupabaseAuthService {
    private final SupabaseProperties properties;
    private final RestClient restClient;

    public SupabaseAuthServiceImpl(
            SupabaseProperties properties,
            @Qualifier("supabaseRestClient") RestClient restClient
    ) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public GoogleIdentity verifyGoogleAccessToken(String accessToken) {
        ensureConfigured();

        try {
            String baseUrl = properties.getUrl().strip().replaceAll("/+$", "");
            SupabaseUser user = restClient
                    .get()
                    .uri(baseUrl + "/auth/v1/user")
                    .header("apikey", properties.getApiKey())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(SupabaseUser.class);

            return toGoogleIdentity(user);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            throw invalidToken();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw invalidToken();
            }
            log.error("Supabase Auth returned status={}", exception.getStatusCode().value());
            throw unavailable();
        } catch (RestClientException exception) {
            log.error("Could not reach Supabase Auth", exception);
            throw unavailable();
        }
    }

    private GoogleIdentity toGoogleIdentity(SupabaseUser user) {
        if (user == null || !StringUtils.hasText(user.email())
                || !StringUtils.hasText(user.emailConfirmedAt()) || !hasGoogleIdentity(user)) {
            throw invalidToken();
        }

        Map<String, Object> metadata = user.userMetadata() == null ? Map.of() : user.userMetadata();
        String fullName = firstText(metadata, "full_name", "name");
        String avatarUrl = firstText(metadata, "avatar_url", "picture");
        return new GoogleIdentity(user.email(), fullName, avatarUrl);
    }

    private boolean hasGoogleIdentity(SupabaseUser user) {
        if (user.identities() != null && user.identities().stream()
                .anyMatch(identity -> "google".equalsIgnoreCase(identity.provider()))) {
            return true;
        }

        Object providers = user.appMetadata() == null ? null : user.appMetadata().get("providers");
        if (providers instanceof List<?> providerList) {
            return providerList.stream().map(String::valueOf).anyMatch("google"::equalsIgnoreCase);
        }
        Object provider = user.appMetadata() == null ? null : user.appMetadata().get("provider");
        return provider != null && "google".equalsIgnoreCase(String.valueOf(provider));
    }

    private String firstText(Map<String, Object> metadata, String... keys) {
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value).strip();
            }
        }
        return null;
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(properties.getUrl()) || !StringUtils.hasText(properties.getApiKey())) {
            log.error("Supabase Auth is not configured");
            throw unavailable();
        }
    }

    private GlobalException invalidToken() {
        return new GlobalException(ErrorCode.INVALID_TOKEN, "Invalid Google login token");
    }

    private GlobalException unavailable() {
        return new GlobalException(ErrorCode.SERVICE_UNAVAILABLE, "Google login is temporarily unavailable");
    }

    private record SupabaseUser(
            String email,
            @JsonProperty("email_confirmed_at") String emailConfirmedAt,
            @JsonProperty("app_metadata") Map<String, Object> appMetadata,
            @JsonProperty("user_metadata") Map<String, Object> userMetadata,
            List<SupabaseIdentity> identities
    ) {
    }

    private record SupabaseIdentity(String provider) {
    }
}
