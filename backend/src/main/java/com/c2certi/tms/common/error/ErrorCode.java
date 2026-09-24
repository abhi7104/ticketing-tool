package com.c2certi.tms.common.error;

/** Stable machine-readable error codes returned in the {@code code} field of problem responses. */
public enum ErrorCode {
  VALIDATION_FAILED,
  INVALID_CREDENTIALS,
  UNAUTHENTICATED,
  FORBIDDEN,
  NOT_FOUND,
  METHOD_NOT_ALLOWED,
  TICKET_NOT_FOUND,
  INVALID_STATUS_TRANSITION,
  TICKET_CLOSED,
  VERSION_CONFLICT,
  PRECONDITION_REQUIRED,
  INTERNAL_ERROR
}
