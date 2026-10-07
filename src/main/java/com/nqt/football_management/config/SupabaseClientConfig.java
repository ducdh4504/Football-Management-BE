package com.nqt.football_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SupabaseClientConfig {

    @Bean("supabaseRestClient")
    public RestClient supabaseRestClient() {
        return RestClient.builder().build();
    }
}
