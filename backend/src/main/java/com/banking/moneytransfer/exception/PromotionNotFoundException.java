package com.banking.moneytransfer.exception;

/**
 * Thrown when a promotion does not exist or is not currently available.
 */
public class PromotionNotFoundException extends RuntimeException {

    private static final int errorCode = 404;

    public PromotionNotFoundException(Long promotionId) {
        super("Promotion " + promotionId + " is not available");
    }

    public int getErrorCode() {
        return errorCode;
    }
}
