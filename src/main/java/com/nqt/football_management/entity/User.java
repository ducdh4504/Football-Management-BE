package com.nqt.football_management.entity;


import com.nqt.football_management.constants.Gender;
import com.nqt.football_management.constants.Role;
import com.nqt.football_management.constants.UserStatus;
import com.nqt.football_management.util.IdGenerator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User implements UserDetails {

    @Id
    @GeneratedValue
    @UuidGenerator
    UUID id;

    @Column(
            unique = true,
            nullable = false,
            updatable = false
    )
    String code;

    @Column(nullable = false, unique = true)
    String email;

    @Column(unique = true)
    String phone;

    LocalDate dateOfBirth;

    @Column(nullable = false)
    String password;

    @Column(nullable = false)
    String fullName;



    @Column(name = "avatar_url", columnDefinition = "TEXT")
    String avatarUrl;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Gender gender;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    UserStatus status = UserStatus.ACTIVE;

    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    boolean emailVerified = false;

    @Column(name = "email_verified_at")
    LocalDateTime emailVerifiedAt;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Role role;


    @CreationTimestamp
    @Column(
            nullable = false,
            updatable = false
    )
    LocalDateTime createdAt;


    @UpdateTimestamp
    LocalDateTime updatedAt;



    @PrePersist
    void generateCode() {

        if (code != null) {
            return;
        }

        if (role == null) {
            throw new IllegalStateException(
                    "Role must not be null"
            );
        }

        String prefix = switch (role) {

            case CLIENT -> "CLI";

            case ADMIN -> "ADM";
        };

        code =
                prefix
                        + "-"
                        + IdGenerator.randomCode(10);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }


    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status != UserStatus.BANNED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.status == UserStatus.ACTIVE;
    }
}
