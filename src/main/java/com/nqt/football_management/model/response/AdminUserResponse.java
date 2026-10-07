package com.nqt.football_management.model.response;



import com.nqt.football_management.constants.Role;
import com.nqt.football_management.constants.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserResponse(UUID id, String code, String email, String phone, String fullName,
                                String avatarUrl, Role role, UserStatus status,
                                LocalDateTime createdAt, LocalDateTime updatedAt) { }
