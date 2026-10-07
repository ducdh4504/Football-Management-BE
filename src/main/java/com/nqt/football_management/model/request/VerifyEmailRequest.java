package com.nqt.football_management.model.request;

import com.nqt.football_management.constants.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerifyEmailRequest {
    @NotBlank(message = ErrorCode.Key.VALIDATION_ERROR)
    String verificationId;

    @NotBlank(message = ErrorCode.Key.VALIDATION_ERROR)
    @Pattern(regexp = "^[0-9]{6}$", message = ErrorCode.Key.INVALID_FORMAT)
    String otp;
}
