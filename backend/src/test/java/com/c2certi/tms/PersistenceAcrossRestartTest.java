package com.c2certi.tms;

import static org.assertj.core.api.Assertions.assertThat;

import com.c2certi.tms.comment.api.CreateCommentRequest;
import com.c2certi.tms.comment.service.CommentService;
import com.c2certi.tms.support.TestDatabase;
import com.c2certi.tms.support.TestSecrets;
import com.c2certi.tms.support.TestUsers;
import com.c2certi.tms.ticket.api.dto.CreateTicketRequest;
import com.c2certi.tms.ticket.api.dto.TicketDetailResponse;
import com.c2certi.tms.ticket.api.dto.UpdateTicketRequest;
import com.c2certi.tms.ticket.domain.Priority;
import com.c2certi.tms.ticket.domain.TicketStatus;
import com.c2certi.tms.ticket.service.TicketCreationService;
import com.c2certi.tms.ticket.service.TicketDetailService;
import com.c2certi.tms.ticket.service.TicketTransitionService;
import com.c2certi.tms.ticket.service.TicketUpdateService;
import com.c2certi.tms.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Starts the application, writes data through the real services, shuts it down completely, starts a
 * fresh instance against the same database and verifies everything is unchanged.
 */
class PersistenceAcrossRestartTest {

  private static ConfigurableApplicationContext start() {
    TestDatabase db = TestDatabase.get();
    // Command-line arguments outrank application.yml placeholders.
    return new SpringApplicationBuilder(TmsApplication.class)
        .profiles("test")
        .run(
            "--server.port=0",
            "--spring.datasource.url=" + db.jdbcUrl(),
            "--spring.datasource.username=" + db.username(),
            "--spring.datasource.password=" + db.password(),
            "--app.jwt.secret=" + TestSecrets.JWT_SECRET);
  }

  @Test
  void ticketsCommentsAndHistorySurviveRestart() {
    String key;
    TicketDetailResponse before;

    try (ConfigurableApplicationContext first = start()) {
      first
          .getBean(JdbcTemplate.class)
          .execute("TRUNCATE ticket_history, ticket_comment, ticket, app_user RESTART IDENTITY");
      TestUsers users = first.getBean(TestUsers.class);
      User alice = users.create("alice", "Alice Moore");
      User bob = users.create("bob", "Bob Singh");

      TicketDetailResponse created =
          first
              .getBean(TicketCreationService.class)
              .create(
                  new CreateTicketRequest(
                      "Server room too hot",
                      "Temperature is above 30 degrees",
                      Priority.HIGH,
                      alice.getId()),
                  alice);
      key = created.key();
      first
          .getBean(TicketUpdateService.class)
          .update(
              key, 0, new UpdateTicketRequest(null, null, Priority.CRITICAL, bob.getId()), alice);
      first
          .getBean(CommentService.class)
          .add(key, new CreateCommentRequest("Facilities notified."), bob);
      TicketDetailResponse afterComment = first.getBean(TicketDetailService.class).getDetail(key);
      first
          .getBean(TicketTransitionService.class)
          .transition(key, afterComment.version(), TicketStatus.IN_PROGRESS, bob);
      before = first.getBean(TicketDetailService.class).getDetail(key);
    }

    try (ConfigurableApplicationContext second = start()) {
      TicketDetailResponse after = second.getBean(TicketDetailService.class).getDetail(key);
      assertThat(after).isEqualTo(before);
      assertThat(after.status()).isEqualTo(TicketStatus.IN_PROGRESS);
      assertThat(after.priority()).isEqualTo(Priority.CRITICAL);
      assertThat(after.assignee().displayName()).isEqualTo("Bob Singh");
      assertThat(after.comments())
          .extracting(c -> c.body())
          .containsExactly("Facilities notified.");
      assertThat(after.history()).hasSize(4);
    }
  }
}
