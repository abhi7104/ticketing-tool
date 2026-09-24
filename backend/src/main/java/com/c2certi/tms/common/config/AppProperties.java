package com.c2certi.tms.common.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Application settings bound from environment variables (see application.yml). */
@ConfigurationProperties("app")
public record AppProperties(Jwt jwt, Cookie cookie, Cors cors, Seed seed) {

  public record Jwt(String secret, Duration ttl) {}

  public record Cookie(boolean secure) {}

  public record Cors(List<String> allowedOrigins) {}

  public record Seed(boolean usersEnabled, String users, String password) {}
}
