package com.c2certi.tms.common.error;

import org.springframework.http.HttpStatus;

public class VersionConflictException extends ApiException {

  public VersionConflictException() {
    super(
        ErrorCode.VERSION_CONFLICT,
        HttpStatus.PRECONDITION_FAILED,
        "Ticket was updated by someone else",
        "This ticket changed since you opened it. Reload to see the latest version.");
  }
}
