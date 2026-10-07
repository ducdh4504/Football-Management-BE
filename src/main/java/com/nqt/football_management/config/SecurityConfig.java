package com.nqt.football_management.config;


import com.nqt.football_management.security.CustomAccessDeniedHandler;
import com.nqt.football_management.security.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class SecurityConfig {
    SecurityProperties securityProperties;
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,

            Converter<Jwt, ? extends AbstractAuthenticationToken>
                    jwtAuthenticationConverter,

            CustomAuthenticationEntryPoint authenticationEntryPoint,

            CustomAccessDeniedHandler accessDeniedHandler
    ) throws Exception {

        List<String> publicEndpoints =
                securityProperties.getPublicEndpoints();

        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)

                .httpBasic(
                        AbstractHttpConfigurer::disable
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .formLogin(
                        AbstractHttpConfigurer::disable
                )

                .exceptionHandling(exception -> exception

                        .authenticationEntryPoint(
                                authenticationEntryPoint
                        )

                        .accessDeniedHandler(
                                accessDeniedHandler
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // PUBLIC API
                        .requestMatchers(
                                publicEndpoints.toArray(
                                        new String[0]
                                )
                        )
                        .permitAll()

                        // PRIVATE API
                        .anyRequest()
                        .authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2

                        .jwt(jwt -> jwt

                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )

                        /*
                         * Quan trọng:
                         *
                         * Error xảy ra trực tiếp trong
                         * Resource Server cũng dùng
                         * custom handlers này.
                         */
                        .authenticationEntryPoint(
                                authenticationEntryPoint
                        )

                        .accessDeniedHandler(
                                accessDeniedHandler
                        )
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }



    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken>
    jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter scopeConverter =
                new JwtGrantedAuthoritiesConverter();

        return jwt -> {

            LinkedHashSet<GrantedAuthority> authorities =
                    new LinkedHashSet<>();

            // =========================
            // 1. SCOPE
            // =========================
            Collection<GrantedAuthority> scopeAuthorities =
                    scopeConverter.convert(jwt);

            if (scopeAuthorities != null) {
                authorities.addAll(scopeAuthorities);
            }

            // =========================
            // 2. ROLE
            // =========================
            Object roleClaim =
                    jwt.getClaims().get("role");

            addRoleAuthorities(
                    authorities,
                    roleClaim
            );

            addExplicitAuthorities(
                    authorities,
                    jwt.getClaims().get("authorities")
            );

            // =========================
            // 3. PRINCIPAL
            // sub = user id / username
            // =========================
            String principalName =
                    jwt.getSubject();

            if (principalName == null ||
                    principalName.isBlank()) {

                throw new IllegalArgumentException(
                        "JWT must contain sub"
                );
            }

            return new JwtAuthenticationToken(
                    jwt,
                    authorities,
                    principalName
            );
        };
    }
    private void addRoleAuthorities(
            Collection<GrantedAuthority> authorities,
            Object roleClaim
    ) {

        if (roleClaim == null) {
            return;
        }

        // role = "ADMIN USER"
        // hoặc role = "ADMIN,USER"
        if (roleClaim instanceof String roleString) {

            Arrays.stream(
                            roleString.split("[,\\s]+")
                    )
                    .map(String::trim)
                    .filter(role -> !role.isBlank())
                    .map(this::normalizeRole)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);

            return;
        }

        // role = ["ADMIN", "USER"]
        if (roleClaim instanceof Collection<?> roles) {

            roles.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(role -> !role.isBlank())
                    .map(this::normalizeRole)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }
    }
    private String normalizeRole(String role) {

        String normalized =
                role.trim()
                        .toUpperCase(Locale.ROOT);

        if (normalized.startsWith("ROLE_")) {
            return normalized;
        }

        return "ROLE_" + normalized;
    }

    private void addExplicitAuthorities(
            Collection<GrantedAuthority> authorities,
            Object authoritiesClaim
    ) {
        if (authoritiesClaim instanceof String authorityString) {
            Arrays.stream(authorityString.split("[,\\s]+"))
                    .map(String::trim)
                    .filter(authority -> !authority.isBlank())
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
            return;
        }

        if (authoritiesClaim instanceof Collection<?> claimedAuthorities) {
            claimedAuthorities.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(authority -> !authority.isBlank())
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }
    }
}
