package com.nqt.football_management.validator;


import com.nqt.football_management.constants.ErrorCode;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target({
        ElementType.FIELD,
        ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = EnumValidatorClass.class
)
public @interface EnumValidator {

    Class<? extends Enum<?>> enumClass();

    String message() default ErrorCode.Key.INVALID_ENUM;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
