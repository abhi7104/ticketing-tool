package com.c2certi.tms.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 * Reads the session token from the HttpOnly cookie. Sign-in and sign-out ignore the cookie so a
 * stale token never blocks them.
 */
public class CookieBearerTokenResolver implements BearerTokenResolver {

  @Override
  public String resolve(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path.endsWith("/api/v1/auth/login") || path.endsWith("/api/v1/auth/logout")) {
      return null;
    }
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (SessionCookieFactory.COOKIE_NAME.equals(cookie.getName())
          && cookie.getValue() != null
          && !cookie.getValue().isBlank()) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
