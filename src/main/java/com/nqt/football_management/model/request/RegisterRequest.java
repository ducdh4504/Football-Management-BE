package com.nqt.football_management.model.request;

import com.nqt.football_management.constants.ErrorCode;
import com.nqt.football_management.constants.Gender;
import com.nqt.football_management.validator.EnumValidator;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {

    @NotBlank(message = ErrorCode.Key.VALIDATION_ERROR)
    String fullName;

    @NotBlank(message = ErrorCode.Key.VALIDATION_ERROR)
    @Email(message = ErrorCode.Key.DATA_INVALID)
    String email;

    @NotBlank(message = ErrorCode.Key.VALIDATION_ERROR)
    @Size(min = 8, message = ErrorCode.Key.DATA_INVALID)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
            message = ErrorCode.Key.INVALID_FORMAT)
    String password;


    @NotNull(message = ErrorCode.Key.VALIDATION_ERROR)
    @Past(message = ErrorCode.Key.DATA_INVALID)
    LocalDate dateOfBirth;

    @NotBlank(message = ErrorCode.Key.VALIDATION_ERROR)
    @EnumValidator(enumClass = Gender.class)
    String gender;

    String avatarUrl;

    @NotBlank(message = ErrorCode.Key.VALIDATION_ERROR)
    @Pattern(
            regexp = "^(0|\\+84)[0-9]{9,10}$",
            message = ErrorCode.Key.INVALID_FORMAT
    )
    String phone;
}
