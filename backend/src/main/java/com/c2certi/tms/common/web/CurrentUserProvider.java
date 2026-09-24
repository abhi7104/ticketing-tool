package com.c2certi.tms.common.web;

import com.c2certi.tms.common.error.UnauthenticatedException;
import com.c2certi.tms.user.domain.User;
import com.c2certi.tms.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Resolves the signed-in, still-active user from the validated session token. */
@Component
public class CurrentUserProvider {

  private final UserRepository users;

  public CurrentUserProvider(UserRepository users) {
    this.users = users;
  }

  public User require() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
      throw new UnauthenticatedException();
    }
    long userId;
    try {
      userId = Long.parseLong(jwt.getSubject());
    } catch (NumberFormatException e) {
      throw new UnauthenticatedException();
    }
    return users.findByIdAndActiveTrue(userId).orElseThrow(UnauthenticatedException::new);
  }
}
