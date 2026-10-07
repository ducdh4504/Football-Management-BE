package com.nqt.football_management.service;

import com.nqt.football_management.entity.User;
import com.nqt.football_management.model.request.RegisterRequest;

import java.util.UUID;

public interface UserRegistrationPersistenceService {
    User createPendingUser(RegisterRequest request);

    User activatePendingUser(UUID userId);

    void discardPendingUser(UUID userId);
}
