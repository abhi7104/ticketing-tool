package com.c2certi.tms.common.error;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {

  public InvalidCredentialsException() {
    super(
        ErrorCode.INVALID_CREDENTIALS,
        HttpStatus.UNAUTHORIZED,
        "Sign-in failed",
        "Invalid username or password.");
  }
}
