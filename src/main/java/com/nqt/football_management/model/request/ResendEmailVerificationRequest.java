package com.nqt.football_management.model.request;

import com.nqt.football_management.constants.ErrorCode;
import jakarta.validation.constraints.NotBlank;
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
public class ResendEmailVerificationRequest {
    @NotBlank(message = ErrorCode.Key.VALIDATION_ERROR)
    String verificationId;
}
