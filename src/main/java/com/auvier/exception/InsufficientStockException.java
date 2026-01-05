package com.auvier.exception;

public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(String productName, int requested, int available) {
        super(String.format("Insufficient stock for '%s': requested %d, available %d",
                productName, requested, available));
    }
}

