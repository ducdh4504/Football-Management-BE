package com.nqt.football_management.security;


import com.nqt.football_management.constants.UserStatus;
import com.nqt.football_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ActiveUserJwtValidator implements OAuth2TokenValidator<Jwt> {
    private final UserRepository users;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        UUID id;
        try {
            id = UUID.fromString(token.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            return invalid();
        }
        return users.existsByIdAndStatus(id, UserStatus.ACTIVE)
                ? OAuth2TokenValidatorResult.success() : invalid();
    }

    private OAuth2TokenValidatorResult invalid() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Account is unavailable or inactive", null));
    }
}
