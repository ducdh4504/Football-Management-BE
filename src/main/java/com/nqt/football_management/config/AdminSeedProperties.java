package com.nqt.football_management.config;

import com.nqt.football_management.constants.ErrorCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.seed.admin")
@Getter
@Setter
public class AdminSeedProperties {
    @NotBlank(message = ErrorCode.Key.DATA_INVALID)
    @Email(message = ErrorCode.Key.DATA_INVALID)
    @Size(max = 255, message = ErrorCode.Key.DATA_INVALID)
    private String email;

    @NotBlank(message = ErrorCode.Key.DATA_INVALID)
    @Size(min = 8, max = 72, message = ErrorCode.Key.DATA_INVALID)
    private String password;

    @NotBlank(message = ErrorCode.Key.DATA_INVALID)
    @Size(max = 255, message = ErrorCode.Key.DATA_INVALID)
    private String fullName = "Administrator";
}
