package com.c2certi.tms.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * One real PostgreSQL 16 per test JVM. Uses Testcontainers when Docker is available (CI); otherwise
 * falls back to embedded PostgreSQL binaries so the suite still runs against real PostgreSQL on
 * machines without Docker. H2 is never used.
 */
public final class TestDatabase {

  private static volatile TestDatabase instance;

  private final String jdbcUrl;
  private final String username;
  private final String password;
  private final String engine;

  private TestDatabase(String jdbcUrl, String username, String password, String engine) {
    this.jdbcUrl = jdbcUrl;
    this.username = username;
    this.password = password;
    this.engine = engine;
  }

  public static TestDatabase get() {
    if (instance == null) {
      synchronized (TestDatabase.class) {
        if (instance == null) {
          instance = start();
        }
      }
    }
    return instance;
  }

  private static TestDatabase start() {
    if (!"embedded".equals(System.getenv("TMS_TEST_DB")) && dockerAvailable()) {
      PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:16-alpine");
      container.start();
      return new TestDatabase(
          container.getJdbcUrl(),
          container.getUsername(),
          container.getPassword(),
          "testcontainers");
    }
    try {
      EmbeddedPostgres pg = EmbeddedPostgres.builder().start();
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    try {
                      pg.close();
                    } catch (IOException ignored) {
                      // shutting down
                    }
                  }));
      return new TestDatabase(pg.getJdbcUrl("postgres", "postgres"), "postgres", "", "embedded");
    } catch (IOException e) {
      throw new UncheckedIOException("Could not start embedded PostgreSQL", e);
    }
  }

  private static boolean dockerAvailable() {
    try {
      return DockerClientFactory.instance().isDockerAvailable();
    } catch (Throwable t) {
      return false;
    }
  }

  public String jdbcUrl() {
    return jdbcUrl;
  }

  public String username() {
    return username;
  }

  public String password() {
    return password;
  }

  public String engine() {
    return engine;
  }
}
