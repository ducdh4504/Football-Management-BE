package com.nqt.football_management.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import com.nqt.football_management.constants.ErrorCode;
import com.nqt.football_management.model.response.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<APIResponse<Object>> accessDenied(AccessDeniedException ex) {
        return response(ErrorCode.FORBIDDEN, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<APIResponse<Object>> invalidParameter(MethodArgumentTypeMismatchException ex) {
        return response(ErrorCode.DATA_INVALID, null);
    }

    @ExceptionHandler(GlobalException.class)
    ResponseEntity<APIResponse<Object>> global(GlobalException ex) {
        ErrorCode ec = ex.getErrorCode() == null ? ErrorCode.UNCATEGORIZED_EXCEPTION : ex.getErrorCode();
        if (ec.getStatus().is5xxServerError()) log.error("Application error code={} message={}", ec.name(), ex.getMessage(), ex);
        else log.warn("Application error code={} message={}", ec.name(), ex.getMessage());
        return response(ec, hasText(ex.getMessage()) ? ex.getMessage() : ec.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<APIResponse<Object>> validation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + validationMessage(error.getDefaultMessage()))
                .toList();
        return response(ErrorCode.VALIDATION_ERROR, errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<APIResponse<Object>> methodValidation(HandlerMethodValidationException ex) {
        var errors = ex.getAllErrors().stream()
                .map(error -> validationMessage(error.getDefaultMessage()))
                .toList();
        return response(ErrorCode.VALIDATION_ERROR, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<APIResponse<Object>> malformed(HttpMessageNotReadableException ex) {
        InvalidFormatException format = findCause(ex, InvalidFormatException.class);
        if (format != null && format.getTargetType() != null && format.getTargetType().isEnum()) {
            return response(ErrorCode.INVALID_ENUM, null);
        }
        return response(ErrorCode.DATA_INVALID, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<APIResponse<Object>> uploadTooLarge(MaxUploadSizeExceededException ex) {
        return response(ErrorCode.DATA_INVALID, "Uploaded file is too large", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<APIResponse<Object>> unexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return response(ErrorCode.UNCATEGORIZED_EXCEPTION, null);
    }

    private ResponseEntity<APIResponse<Object>> response(ErrorCode errorCode, Object data) {
        return response(errorCode, errorCode.getMessage(), data);
    }

    private ResponseEntity<APIResponse<Object>> response(ErrorCode errorCode, String message, Object data) {
        return response(errorCode.getStatus(), message, data);
    }

    private ResponseEntity<APIResponse<Object>> response(HttpStatus status, String message, Object data) {
        return ResponseEntity.status(status).body(APIResponse.response(status.value(), message, data, false));
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private String validationMessage(String errorCodeKey) {
        try {
            return ErrorCode.valueOf(errorCodeKey).getMessage();
        } catch (IllegalArgumentException | NullPointerException ex) {
            log.warn("Unknown validation error code key={}", errorCodeKey);
            return ErrorCode.VALIDATION_ERROR.getMessage();
        }
    }

    private <T extends Throwable> T findCause(Throwable t, Class<T> type) {
        Throwable current = t;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
