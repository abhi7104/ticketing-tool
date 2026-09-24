package com.c2certi.tms.common.error;

import java.util.List;
import org.springframework.http.HttpStatus;

/** Validation failure detected in service code (e.g. an assignee that does not exist). */
public class FieldValidationException extends ApiException {

  private final List<ApiFieldError> errors;

  public FieldValidationException(String field, String message) {
    this(List.of(new ApiFieldError(field, message)));
  }

  public FieldValidationException(List<ApiFieldError> errors) {
    super(
        ErrorCode.VALIDATION_FAILED,
        HttpStatus.BAD_REQUEST,
        "Please correct the highlighted fields",
        errors.size() == 1
            ? "1 field needs attention."
            : errors.size() + " fields need attention.");
    this.errors = List.copyOf(errors);
  }

  public List<ApiFieldError> errors() {
    return errors;
  }
}
