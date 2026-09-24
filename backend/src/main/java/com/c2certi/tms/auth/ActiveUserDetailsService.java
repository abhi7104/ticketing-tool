package com.c2certi.tms.auth;

import com.c2certi.tms.user.domain.User;
import com.c2certi.tms.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads active users for password sign-in. */
@Service
public class ActiveUserDetailsService implements UserDetailsService {

  private final UserRepository users;

  public ActiveUserDetailsService(UserRepository users) {
    this.users = users;
  }

  @Override
  public UserDetails loadUserByUsername(String username) {
    User user =
        users
            .findByUsernameIgnoreCase(username)
            .filter(User::isActive)
            .orElseThrow(() -> new UsernameNotFoundException("not found"));
    return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
        .password(user.getPasswordHash())
        .authorities("ROLE_USER")
        .build();
  }
}
