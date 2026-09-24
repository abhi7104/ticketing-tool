package com.c2certi.tms.common.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Base type for expected business errors that map directly to a problem response. */
public class ApiException extends RuntimeException {

  private final ErrorCode code;
  private final HttpStatus status;
  private final String title;

  public ApiException(ErrorCode code, HttpStatus status, String title, String detail) {
    super(detail);
    this.code = code;
    this.status = status;
    this.title = title;
  }

  public ErrorCode code() {
    return code;
  }

  public HttpStatus status() {
    return status;
  }

  public String title() {
    return title;
  }

  public String detail() {
    return getMessage();
  }

  /** Extra problem properties added to the response body. */
  public Map<String, Object> properties() {
    return Map.of();
  }
}
