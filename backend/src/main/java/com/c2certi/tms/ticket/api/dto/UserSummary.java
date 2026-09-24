package com.c2certi.tms.ticket.api.dto;

import com.c2certi.tms.user.domain.User;

public record UserSummary(Long id, String displayName, String email) {

  public static UserSummary from(User user) {
    return new UserSummary(user.getId(), user.getDisplayName(), user.getEmail());
  }
}
