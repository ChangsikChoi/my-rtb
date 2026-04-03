package com.example.ad_manager.exception;

import lombok.Getter;

@Getter
public abstract class ApiException extends RuntimeException {

  private final String code;

  protected ApiException(String code, String message) {
    super(message);
    this.code = code;
  }

  protected ApiException(String code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

}
