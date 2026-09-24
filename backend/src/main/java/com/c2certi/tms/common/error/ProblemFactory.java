package com.c2certi.tms.common.error;

import com.c2certi.tms.common.web.RequestIdFilter;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Builds RFC 9457 problem bodies with the project's {@code code}/{@code errors}/{@code traceId}.
 */
public final class ProblemFactory {

  private ProblemFactory() {}

  public static ProblemDetail create(
      HttpStatus status, ErrorCode code, String title, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setProperty("code", code.name());
    String traceId = MDC.get(RequestIdFilter.MDC_KEY);
    if (traceId != null) {
      problem.setProperty("traceId", traceId);
    }
    return problem;
  }

  public static ProblemDetail validation(List<ApiFieldError> errors) {
    ProblemDetail problem =
        create(
            HttpStatus.BAD_REQUEST,
            ErrorCode.VALIDATION_FAILED,
            "Please correct the highlighted fields",
            errors.size() == 1
                ? "1 field needs attention."
                : errors.size() + " fields need attention.");
    problem.setProperty("errors", errors);
    return problem;
  }

  public static ProblemDetail from(ApiException ex) {
    ProblemDetail problem = create(ex.status(), ex.code(), ex.title(), ex.detail());
    for (Map.Entry<String, Object> e : ex.properties().entrySet()) {
      problem.setProperty(e.getKey(), e.getValue());
    }
    if (ex instanceof FieldValidationException fve) {
      problem.setProperty("errors", fve.errors());
    }
    return problem;
  }
}
