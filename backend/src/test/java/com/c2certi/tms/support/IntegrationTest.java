package com.c2certi.tms.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Base class: full application context against real PostgreSQL, clean tables per test. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

  @Autowired protected MockMvc mvc;
  @Autowired protected JdbcTemplate jdbc;
  @Autowired protected TestUsers users;
  @Autowired protected ApiClient api;
  @Autowired protected TicketFixtures fixtures;

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    TestDatabase db = TestDatabase.get();
    registry.add("spring.datasource.url", db::jdbcUrl);
    registry.add("spring.datasource.username", db::username);
    registry.add("spring.datasource.password", db::password);
    registry.add("app.jwt.secret", () -> TestSecrets.JWT_SECRET);
  }

  @BeforeEach
  void cleanDatabase() {
    jdbc.execute("TRUNCATE ticket_history, ticket_comment, ticket, app_user RESTART IDENTITY");
  }
}
