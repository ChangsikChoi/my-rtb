package com.example.ad_manager.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ApiErrorResponse(
    String code,
    String message,
    List<ApiValidationError> errors,
    LocalDateTime timestamp
) {

}
