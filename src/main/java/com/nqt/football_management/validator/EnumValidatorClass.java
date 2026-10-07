package com.nqt.football_management.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValidatorClass
        implements ConstraintValidator<EnumValidator, String> {

    private Set<String> allowedValues;

    @Override
    public void initialize(
            EnumValidator annotation
    ) {

        allowedValues =
                Arrays.stream(
                                annotation
                                        .enumClass()
                                        .getEnumConstants()
                        )
                        .map(Enum::name)
                        .map(value ->
                                value.toUpperCase(Locale.ROOT)
                        )
                        .collect(
                                Collectors.toUnmodifiableSet()
                        );

    }

    @Override
    public boolean isValid(
            String value,
            ConstraintValidatorContext context
    ) {

        /*
         * null / blank:
         * để @NotBlank / @NotNull xử lý
         */
        if (value == null || value.isBlank()) {
            return true;
        }

        return allowedValues.contains(
                value.trim()
                        .toUpperCase(Locale.ROOT)
        );
    }
}
