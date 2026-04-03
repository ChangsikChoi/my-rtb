package com.example.ad_manager.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(Include.NON_NULL)
public record ApiErrorResponse(
    String code,
    String message,
    List<ApiValidationError> validationErrors,
    LocalDateTime timestamp
) {

}
