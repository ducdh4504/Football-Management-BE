package com.nqt.football_management.model.request;

import com.nqt.football_management.constants.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class GoogleLoginRequest {
    @NotBlank(message = ErrorCode.Key.VALIDATION_ERROR)
    String accessToken;
}
