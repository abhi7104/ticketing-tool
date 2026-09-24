package com.c2certi.tms.common.error;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Maps every error to an RFC 9457 problem response with a stable {@code code}. Messages are
 * user-friendly; technical details go to the server log only.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private static final Set<String> REQUIRED_CODES =
      Set.of("NotBlank", "NotNull", "NotEmpty", "Pattern");

  private static final Map<String, String> FIELD_LABELS =
      Map.of(
          "title", "Title",
          "description", "Description",
          "priority", "Priority",
          "assigneeId", "Assignee",
          "body", "Comment",
          "targetStatus", "Status",
          "status", "Status",
          "username", "Username",
          "password", "Password");

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ProblemDetail> handleApi(ApiException ex) {
    return respond(ProblemFactory.from(ex));
  }

  @ExceptionHandler(BindException.class)
  ResponseEntity<ProblemDetail> handleBind(BindException ex) {
    Map<String, FieldError> perField = new LinkedHashMap<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      FieldError existing = perField.get(fe.getField());
      if (existing == null || (isRequired(fe) && !isRequired(existing))) {
        perField.put(fe.getField(), fe);
      }
    }
    List<ApiFieldError> errors = new ArrayList<>();
    perField
        .values()
        .forEach(
            fe ->
                errors.add(
                    new ApiFieldError(
                        "anyFieldPresent".equals(fe.getField()) ? "request" : fe.getField(),
                        messageFor(fe))));
    for (ObjectError oe : ex.getBindingResult().getGlobalErrors()) {
      errors.add(new ApiFieldError("request", oe.getDefaultMessage()));
    }
    return respond(ProblemFactory.validation(errors));
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ResponseEntity<ProblemDetail> handleMethodValidation(HandlerMethodValidationException ex) {
    List<ApiFieldError> errors =
        ex.getParameterValidationResults().stream()
            .flatMap(
                r ->
                    r.getResolvableErrors().stream()
                        .map(
                            e ->
                                new ApiFieldError(
                                    r.getMethodParameter().getParameterName(),
                                    e.getDefaultMessage())))
            .toList();
    return respond(ProblemFactory.validation(errors));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    String field = ex.getName();
    return respond(
        ProblemFactory.validation(
            List.of(new ApiFieldError(field, invalidValueMessage(field, ex.getRequiredType())))));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex) {
    Throwable cause = ex.getCause();
    ApiFieldError error;
    if (cause instanceof UnrecognizedPropertyException upe) {
      String field = upe.getPropertyName();
      String message =
          "status".equals(field)
              ? "Status can't be changed here. Use the status action instead."
              : "Unknown field '" + field + "'.";
      error = new ApiFieldError(field, message);
    } else if (cause instanceof InvalidFormatException ife) {
      String field = path(ife);
      error = new ApiFieldError(field, invalidValueMessage(field, ife.getTargetType()));
    } else if (cause instanceof MismatchedInputException mie && !mie.getPath().isEmpty()) {
      String field = path(mie);
      error = new ApiFieldError(field, invalidValueMessage(field, mie.getTargetType()));
    } else {
      error = new ApiFieldError("request", "The request body is missing or malformed.");
    }
    return respond(ProblemFactory.validation(List.of(error)));
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  ResponseEntity<ProblemDetail> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
    return respond(ProblemFactory.from(new VersionConflictException()));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex) {
    return respond(
        ProblemFactory.create(
            HttpStatus.NOT_FOUND,
            ErrorCode.NOT_FOUND,
            "Not found",
            "The requested resource doesn't exist."));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ProblemDetail> handleMethod(HttpRequestMethodNotSupportedException ex) {
    return respond(
        ProblemFactory.create(
            HttpStatus.METHOD_NOT_ALLOWED,
            ErrorCode.METHOD_NOT_ALLOWED,
            "Action not supported",
            "This action isn't supported."));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ProblemDetail> handleMediaType(HttpMediaTypeNotSupportedException ex) {
    return respond(
        ProblemFactory.validation(
            List.of(new ApiFieldError("request", "The request must be sent as JSON."))));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
    log.error("Unexpected error", ex);
    return respond(
        ProblemFactory.create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.INTERNAL_ERROR,
            "Something went wrong",
            "Please try again."));
  }

  private static ResponseEntity<ProblemDetail> respond(ProblemDetail problem) {
    return ResponseEntity.status(problem.getStatus())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  private static boolean isRequired(FieldError fe) {
    return fe.getCode() != null && REQUIRED_CODES.contains(fe.getCode());
  }

  private static String messageFor(FieldError fe) {
    if (fe.isBindingFailure()) {
      return invalidValueMessage(fe.getField(), null);
    }
    return fe.getDefaultMessage();
  }

  private static String path(JsonMappingException ex) {
    return ex.getPath().stream()
        .map(r -> r.getFieldName() != null ? r.getFieldName() : String.valueOf(r.getIndex()))
        .collect(Collectors.joining("."));
  }

  private static String invalidValueMessage(String field, Class<?> type) {
    String label = FIELD_LABELS.getOrDefault(field, field);
    if (type != null && type.isEnum()) {
      String allowed =
          Arrays.stream(type.getEnumConstants())
              .map(c -> ((Enum<?>) c).name())
              .collect(Collectors.joining(", "));
      return label + " must be one of " + allowed + ".";
    }
    if ("status".equals(field)) {
      return "Status must be one of OPEN, IN_PROGRESS, RESOLVED, CLOSED.";
    }
    return label + " has an invalid value.";
  }
}
