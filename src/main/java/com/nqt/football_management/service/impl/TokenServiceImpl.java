package com.nqt.football_management.service.impl;

import com.nqt.football_management.config.SecurityProperties;
import com.nqt.football_management.entity.User;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TokenServiceImpl {

    JwtEncoder jwtEncoder;

    SecurityProperties securityProperties;


    public String generateAccessToken(User user) {

        Instant now = Instant.now();

        List<String> authorities =
                user.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();

        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(securityProperties.getIssuer())
                        .audience(securityProperties.getAudiences())
                        .issuedAt(now)
                        .expiresAt(now.plus(1, ChronoUnit.HOURS))
                        .subject(user.getId().toString())
                        .claim("email", user.getEmail())
                        .claim("code", user.getCode())
                        .claim("role", user.getRole().name())
                        .claim("authorities", authorities)
                        .build();

        JwsHeader header =
                JwsHeader
                        .with(MacAlgorithm.HS512)
                        .build();

        return jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                header,
                                claims
                        )
                )
                .getTokenValue();
    }
}
