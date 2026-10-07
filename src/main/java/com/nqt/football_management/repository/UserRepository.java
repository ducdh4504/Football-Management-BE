package com.nqt.football_management.repository;

import com.nqt.football_management.constants.Role;
import com.nqt.football_management.constants.UserStatus;
import com.nqt.football_management.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    boolean existsByIdAndStatus(UUID id, UserStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByRole(Role role);

    User findUserByEmail(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByPhone(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Modifying
    @Query("""
            update User u
            set u.emailVerified = true, u.emailVerifiedAt = :verifiedAt
            where u.status = :status and u.emailVerified = false
            """)
    int markExistingActiveUsersAsEmailVerified(
            @Param("status") UserStatus status,
            @Param("verifiedAt") LocalDateTime verifiedAt
    );
}
