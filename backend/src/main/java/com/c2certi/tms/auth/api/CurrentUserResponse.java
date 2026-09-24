package com.c2certi.tms.auth.api;

import com.c2certi.tms.user.domain.User;

public record CurrentUserResponse(Long id, String username, String displayName, String email) {

  public static CurrentUserResponse from(User user) {
    return new CurrentUserResponse(
        user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail());
  }
}
