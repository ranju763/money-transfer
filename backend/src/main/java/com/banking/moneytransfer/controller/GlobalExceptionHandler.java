package com.banking.moneytransfer.controller;

import com.banking.moneytransfer.dto.ErrorResponse;
import com.banking.moneytransfer.exception.AccountNotActiveException;
import com.banking.moneytransfer.exception.AccountNotFoundException;
import com.banking.moneytransfer.exception.DuplicateTransferException;
import com.banking.moneytransfer.exception.InsufficientBalanceException;
import com.banking.moneytransfer.exception.InsufficientCoinsException;
import com.banking.moneytransfer.exception.PromotionNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all controllers
 */

// basePackage property ensures handleGenericException method don't handle framework specific Exception.
@RestControllerAdvice(basePackages = "com.banking.moneytransfer")
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleAccountNotFoundException(AccountNotFoundException ex,
                                                                        HttpServletRequest request) {
        log.error("Account not found: {}", ex.getMessage());

        return new ErrorResponse(ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AccountNotActiveException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccountNotActiveException(AccountNotActiveException ex,
                                                                         HttpServletRequest request) {
        log.error("Account not active: {}", ex.getMessage());

        return new ErrorResponse(ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInsufficientBalanceException(InsufficientBalanceException ex,
                                                                            HttpServletRequest request) {
        log.error("Insufficient balance: {}", ex.getMessage());

        return new ErrorResponse(ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateTransferException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateTransferException(DuplicateTransferException ex,
                                                                          HttpServletRequest request) {
        log.error("Duplicate transfer: {}", ex.getMessage());
        return new ErrorResponse(ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InsufficientCoinsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInsufficientCoinsException(InsufficientCoinsException ex,
                                                         HttpServletRequest request) {
        log.error("Insufficient coins: {}", ex.getMessage());

        return new ErrorResponse(ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(PromotionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handlePromotionNotFoundException(PromotionNotFoundException ex,
                                                          HttpServletRequest request) {
        log.error("Promotion not found: {}", ex.getMessage());

        return new ErrorResponse(ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex,
                                                                        HttpServletRequest request) {
        log.error("Validation error: {}", ex.getMessage());

        return new ErrorResponse(422, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex,
                                                                    HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Validation errors: {}", errors);

        return new ErrorResponse(422, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAccessDeniedException(Exception ex,
                                                HttpServletRequest request) {
        log.error("Access denied: {}", ex.getMessage(), ex);

        return new ErrorResponse(401, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(Exception ex,
                                                                HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return new ErrorResponse(500, ex.getMessage(), request.getRequestURI());
    }
}