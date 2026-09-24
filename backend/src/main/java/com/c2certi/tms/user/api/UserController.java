package com.c2certi.tms.user.api;

import com.c2certi.tms.ticket.api.dto.UserSummary;
import com.c2certi.tms.user.repository.UserRepository;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserRepository users;

  public UserController(UserRepository users) {
    this.users = users;
  }

  /** Active users, for the assignee picker. */
  @GetMapping
  @Transactional(readOnly = true)
  public List<UserSummary> list() {
    return users.findAllByActiveTrueOrderByDisplayNameAsc().stream()
        .map(UserSummary::from)
        .toList();
  }
}
