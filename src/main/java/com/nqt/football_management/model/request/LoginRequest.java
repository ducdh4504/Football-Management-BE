package com.nqt.football_management.model.request;

import com.nqt.football_management.constants.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginRequest {
    @NotBlank(message = ErrorCode.Key.VALIDATION_ERROR)
    String email;

    @NotBlank(message = ErrorCode.Key.VALIDATION_ERROR)
    String password;
}
