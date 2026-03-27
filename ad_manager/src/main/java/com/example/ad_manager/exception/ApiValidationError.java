package com.example.ad_manager.exception;

public record ApiValidationError(
    String field,
    String message
) {

}
