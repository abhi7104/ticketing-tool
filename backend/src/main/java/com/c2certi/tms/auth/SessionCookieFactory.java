package com.c2certi.tms.auth;

import com.c2certi.tms.common.config.AppProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Builds the HttpOnly session cookie that carries the signed token. */
@Component
public class SessionCookieFactory {

  public static final String COOKIE_NAME = "TMS_SESSION";

  private final boolean secure;

  public SessionCookieFactory(AppProperties props) {
    this.secure = props.cookie() == null || props.cookie().secure();
  }

  public ResponseCookie create(String token, Duration ttl) {
    return base(token).maxAge(ttl).build();
  }

  public ResponseCookie clear() {
    return base("").maxAge(0).build();
  }

  private ResponseCookie.ResponseCookieBuilder base(String value) {
    return ResponseCookie.from(COOKIE_NAME, value)
        .httpOnly(true)
        .secure(secure)
        .sameSite("Strict")
        .path("/");
  }
}
