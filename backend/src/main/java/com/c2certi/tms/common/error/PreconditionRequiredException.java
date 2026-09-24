package com.c2certi.tms.common.error;

import org.springframework.http.HttpStatus;

public class PreconditionRequiredException extends ApiException {

  public PreconditionRequiredException() {
    super(
        ErrorCode.PRECONDITION_REQUIRED,
        HttpStatus.PRECONDITION_REQUIRED,
        "Missing version information",
        "Please reload the ticket and try again.");
  }
}
