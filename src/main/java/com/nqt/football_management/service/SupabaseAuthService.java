package com.nqt.football_management.service;

public interface SupabaseAuthService {
    GoogleIdentity verifyGoogleAccessToken(String accessToken);

    record GoogleIdentity(String email, String fullName, String avatarUrl) {
    }
}
