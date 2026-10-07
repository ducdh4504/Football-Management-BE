package com.nqt.football_management.exception;

import com.nqt.football_management.constants.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GlobalException extends RuntimeException {

    ErrorCode errorCode;

    // Dùng message mặc định từ ErrorCode
    public GlobalException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // Dùng custom message
    public GlobalException(
            ErrorCode errorCode,
            String customMessage
    ) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
