package com.nqt.football_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory
    ) {

        RedisTemplate<String, Object> template =
                new RedisTemplate<>();

        template.setConnectionFactory(
                connectionFactory
        );

        /*
         * =========================
         * KEY
         * =========================
         *
         * user:123
         * refresh-token:abc
         */
        StringRedisSerializer stringSerializer =
                new StringRedisSerializer();

        template.setKeySerializer(
                stringSerializer
        );

        template.setHashKeySerializer(
                stringSerializer
        );


        /*
         * =========================
         * VALUE
         * =========================
         *
         * Object -> JSON
         */
        RedisSerializer<Object> jsonSerializer =
                RedisSerializer.json();

        template.setValueSerializer(
                jsonSerializer
        );

        template.setHashValueSerializer(
                jsonSerializer
        );


        template.afterPropertiesSet();

        return template;
    }
}