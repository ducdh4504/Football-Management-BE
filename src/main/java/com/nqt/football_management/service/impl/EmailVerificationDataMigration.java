package com.nqt.football_management.service.impl;

import com.nqt.football_management.constants.UserStatus;
import com.nqt.football_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationDataMigration implements ApplicationRunner {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int migrated = userRepository.markExistingActiveUsersAsEmailVerified(
                UserStatus.ACTIVE,
                LocalDateTime.now()
        );
        if (migrated > 0) {
            log.info("Marked {} existing active account(s) as email verified", migrated);
        }
    }
}
