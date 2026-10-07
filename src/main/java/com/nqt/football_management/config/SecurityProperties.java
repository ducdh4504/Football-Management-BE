package com.nqt.football_management.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;


@Configuration
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SecurityProperties {
    private List<String> publicEndpoints = new ArrayList<>();

    private String jwtSecret;

    private String issuer;

    private List<String> audiences = new ArrayList<>();
}
