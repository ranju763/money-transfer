package com.banking.moneytransfer.exception;

/**
 * Thrown when a user tries to redeem a promotion without enough SwiftCoins.
 */
public class InsufficientCoinsException extends RuntimeException {

    private static final int errorCode = 400;

    public InsufficientCoinsException(String message) {
        super(message);
    }

    public int getErrorCode() {
        return errorCode;
    }
}
