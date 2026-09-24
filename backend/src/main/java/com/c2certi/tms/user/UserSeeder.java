package com.c2certi.tms.user;

import com.c2certi.tms.common.config.AppProperties;
import com.c2certi.tms.user.domain.User;
import com.c2certi.tms.user.repository.UserRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the pre-defined user accounts from {@code APP_SEED_USERS} (format {@code username:Display
 * Name:email;...}) with the initial password from {@code APP_SEED_USER_PASSWORD}. Existing
 * usernames are left untouched, so running it again is safe.
 */
@Component
@ConditionalOnProperty(name = "app.seed.users-enabled", havingValue = "true")
public class UserSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

  private final AppProperties props;
  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  public UserSeeder(
      AppProperties props, UserRepository users, PasswordEncoder passwordEncoder, Clock clock) {
    this.props = props;
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    String spec = props.seed().users();
    String password = props.seed().password();
    if (spec == null || spec.isBlank()) {
      log.warn("User seeding enabled but APP_SEED_USERS is empty; no users created");
      return;
    }
    if (password == null || password.isBlank()) {
      throw new IllegalStateException(
          "APP_SEED_USER_PASSWORD must be set when APP_SEED_USERS_ENABLED=true");
    }
    String hash = null;
    int created = 0;
    for (String entry : spec.split(";")) {
      if (entry.isBlank()) {
        continue;
      }
      String[] parts = entry.trim().split(":");
      if (parts.length != 3) {
        throw new IllegalStateException(
            "Invalid APP_SEED_USERS entry (expected username:Display Name:email): " + parts[0]);
      }
      String username = parts[0].trim().toLowerCase();
      if (users.existsByUsernameIgnoreCase(username)) {
        continue;
      }
      if (hash == null) {
        hash = passwordEncoder.encode(password);
      }
      users.save(new User(username, parts[1].trim(), parts[2].trim(), hash, clock.instant()));
      created++;
    }
    log.info("User seeding complete; {} new user(s) created", created);
  }
}
