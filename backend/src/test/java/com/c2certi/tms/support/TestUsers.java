package com.c2certi.tms.support;

import com.c2certi.tms.user.domain.User;
import com.c2certi.tms.user.repository.UserRepository;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates users directly in the database; all share {@link TestSecrets#USER_PASSWORD}. */
@Component
public class TestUsers {

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private String hash;

  public TestUsers(UserRepository users, PasswordEncoder encoder) {
    this.users = users;
    this.encoder = encoder;
  }

  public User create(String username) {
    return create(username, capitalize(username) + " Tester");
  }

  public User create(String username, String displayName) {
    if (hash == null) {
      hash = encoder.encode(TestSecrets.USER_PASSWORD);
    }
    return users.save(
        new User(username, displayName, username + "@example.test", hash, Instant.now()));
  }

  public User deactivated(String username) {
    User user = create(username);
    user.deactivate();
    return users.save(user);
  }

  private static String capitalize(String s) {
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }
}
