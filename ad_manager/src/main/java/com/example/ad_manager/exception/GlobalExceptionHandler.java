package com.example.ad_manager.exception;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception) {
    List<ApiValidationError> errors = exception.getBindingResult()
        .getAllErrors()
        .stream()
        .map(error -> {
          String field = error instanceof FieldError fieldError
              ? fieldError.getField()
              : error.getObjectName();
          return new ApiValidationError(field, error.getDefaultMessage());
        })
        .toList();

    return ResponseEntity.badRequest().body(new ApiErrorResponse(
        "VALIDATION_ERROR",
        "Request validation failed",
        errors,
        LocalDateTime.now()
    ));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception) {
    return ResponseEntity.badRequest().body(new ApiErrorResponse(
        "INVALID_REQUEST_BODY",
        "Request body is invalid or malformed",
        List.of(),
        LocalDateTime.now()
    ));
  }

  @ExceptionHandler(DuplicateCampaignNameException.class)
  public ResponseEntity<ApiErrorResponse> handleDuplicateCampaignName(
      DuplicateCampaignNameException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
        "DUPLICATE_CAMPAIGN_NAME",
        exception.getMessage(),
        List.of(),
        LocalDateTime.now()
    ));
  }
}
