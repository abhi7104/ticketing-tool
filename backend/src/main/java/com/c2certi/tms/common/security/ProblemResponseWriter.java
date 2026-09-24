package com.c2certi.tms.common.security;

import com.c2certi.tms.common.error.ErrorCode;
import com.c2certi.tms.common.error.ProblemFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/** Writes problem JSON from security filters, which run before controller advice. */
@Component
public class ProblemResponseWriter {

  private final ObjectMapper objectMapper;

  public ProblemResponseWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void write(
      HttpServletResponse response, HttpStatus status, ErrorCode code, String title, String detail)
      throws IOException {
    ProblemDetail problem = ProblemFactory.create(status, code, title, detail);
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
