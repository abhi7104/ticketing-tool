package com.c2certi.tms.support;

import com.c2certi.tms.auth.JwtService;
import com.c2certi.tms.auth.SessionCookieFactory;
import com.c2certi.tms.user.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Builds authenticated MockMvc requests for a given user (session cookie + CSRF token). */
@Component
public class ApiClient {

  private final JwtService jwtService;
  private final ObjectMapper json;

  public ApiClient(JwtService jwtService, ObjectMapper json) {
    this.jwtService = jwtService;
    this.json = json;
  }

  /**
   * Sends a real double-submit CSRF token (cookie + header), exactly like the browser client. The
   * spring-security-test {@code csrf()} helper is avoided because it swaps the filter's shared
   * token repository for the rest of the test run.
   */
  public static RequestPostProcessor csrf() {
    return request -> {
      String token = UUID.randomUUID().toString();
      Cookie[] existing = request.getCookies();
      Cookie[] cookies =
          existing == null ? new Cookie[1] : java.util.Arrays.copyOf(existing, existing.length + 1);
      cookies[cookies.length - 1] = new Cookie("XSRF-TOKEN", token);
      request.setCookies(cookies);
      request.addHeader("X-XSRF-TOKEN", token);
      return request;
    };
  }

  public Cookie sessionFor(User user) {
    return new Cookie(SessionCookieFactory.COOKIE_NAME, jwtService.issue(user));
  }

  public MockHttpServletRequestBuilder get(User user, String url, Object... vars) {
    return MockMvcRequestBuilders.get(url, vars).cookie(sessionFor(user));
  }

  public MockHttpServletRequestBuilder post(User user, String url, Object body, Object... vars) {
    return withBody(MockMvcRequestBuilders.post(url, vars), user, body);
  }

  public MockHttpServletRequestBuilder patch(User user, String url, Object body, Object... vars) {
    return withBody(MockMvcRequestBuilders.patch(url, vars), user, body);
  }

  public String toJson(Object body) {
    try {
      return body instanceof String s ? s : json.writeValueAsString(body);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private MockHttpServletRequestBuilder withBody(
      MockHttpServletRequestBuilder builder, User user, Object body) {
    builder.contentType(MediaType.APPLICATION_JSON).with(csrf());
    if (user != null) {
      builder.cookie(sessionFor(user));
    }
    if (body != null) {
      builder.content(toJson(body));
    }
    return builder;
  }
}
