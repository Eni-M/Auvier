package com.auvier.exceptions;

public class ProductVariantConflictException extends ConflictException {
  public ProductVariantConflictException(String message) {
    super(message);
  }
}
