package com.nqt.football_management.config;

import com.nqt.football_management.security.ActiveUserJwtValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class JWTConfig {

    @Value("${app.security.jwt-secret}")
    private String signerKey;

    @Value("${app.security.issuer}")
    private String issuer;

    private SecretKey getSecretKey() {
        byte[] keyBytes =
                Base64.getDecoder()
                        .decode(signerKey);

        return new SecretKeySpec(
                keyBytes,
                "HmacSHA512"
        );
    }

    @Bean
    public JwtDecoder jwtDecoder(ActiveUserJwtValidator activeUserValidator) {

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder
                        .withSecretKey(getSecretKey())
                        .macAlgorithm(MacAlgorithm.HS512)
                        .build();

        OAuth2TokenValidator<Jwt> validator =
                JwtValidators.createDefaultWithIssuer(
                        issuer
                );

        decoder.setJwtValidator(token -> {
            var result = validator.validate(token);
            return result.hasErrors() ? result : activeUserValidator.validate(token);
        });

        return decoder;
    }

    @Bean
    public JwtEncoder jwtEncoder() {

        return NimbusJwtEncoder
                .withSecretKey(getSecretKey())
                .algorithm(MacAlgorithm.HS512)
                .build();
    }
}
