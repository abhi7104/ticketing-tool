package com.c2certi.tms.common.error;

import org.springframework.http.HttpStatus;

public class UnauthenticatedException extends ApiException {

  public UnauthenticatedException() {
    super(
        ErrorCode.UNAUTHENTICATED,
        HttpStatus.UNAUTHORIZED,
        "Session expired",
        "Please sign in again.");
  }
}
