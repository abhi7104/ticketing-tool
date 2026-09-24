package com.c2certi.tms.ticket.service;

import com.c2certi.tms.common.error.FieldValidationException;
import com.c2certi.tms.user.domain.User;
import com.c2certi.tms.user.repository.UserRepository;
import org.springframework.stereotype.Component;

/** Resolves an assignee id to an active user or fails with a field-level validation error. */
@Component
public class AssigneeResolver {

  private final UserRepository users;

  public AssigneeResolver(UserRepository users) {
    this.users = users;
  }

  public User resolve(Long assigneeId) {
    return users
        .findByIdAndActiveTrue(assigneeId)
        .orElseThrow(
            () -> new FieldValidationException("assigneeId", "Please choose a valid assignee."));
  }
}
