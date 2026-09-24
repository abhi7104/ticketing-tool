package com.c2certi.tms.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c2certi.tms.support.IntegrationTest;
import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.ticket.domain.TicketStatus;
import com.c2certi.tms.user.domain.User;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommentIntegrationTest extends IntegrationTest {

  private User alice;
  private User bob;
  private Ticket ticket;

  @BeforeEach
  void setUp() {
    alice = users.create("alice", "Alice Moore");
    bob = users.create("bob", "Bob Singh");
    ticket = fixtures.create(alice);
  }

  private long commentCount() {
    return jdbc.queryForObject("SELECT count(*) FROM ticket_comment", Long.class);
  }

  @Test
  void addsCommentsInOrderWithAuthor() throws Exception {
    jdbc.update(
        "UPDATE ticket SET updated_at = now() - interval '1 day' WHERE id = ?", ticket.getId());

    mvc.perform(
            api.post(
                alice,
                "/api/v1/tickets/{key}/comments",
                Map.of("body", "  Restarted the printer.  "),
                ticket.getKey()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.body").value("Restarted the printer."))
        .andExpect(jsonPath("$.author.displayName").value("Alice Moore"))
        .andExpect(jsonPath("$.createdAt").isNotEmpty());
    mvc.perform(
            api.post(
                bob,
                "/api/v1/tickets/{key}/comments",
                Map.of("body", "Still jammed."),
                ticket.getKey()))
        .andExpect(status().isCreated());

    mvc.perform(api.get(alice, "/api/v1/tickets/{key}/comments", ticket.getKey()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].body", contains("Restarted the printer.", "Still jammed.")))
        .andExpect(jsonPath("$[1].author.displayName").value("Bob Singh"));
    mvc.perform(api.get(alice, "/api/v1/tickets/{key}", ticket.getKey()))
        .andExpect(jsonPath("$.comments", hasSize(2)));

    Boolean bumped =
        jdbc.queryForObject(
            "SELECT updated_at > now() - interval '1 minute' FROM ticket WHERE id = ?",
            Boolean.class,
            ticket.getId());
    assertThat(bumped).isTrue();
  }

  @Test
  void storesMarkupVerbatim() throws Exception {
    String body = "<script>alert('x')</script> & <b>bold</b>";
    mvc.perform(
            api.post(
                alice, "/api/v1/tickets/{key}/comments", Map.of("body", body), ticket.getKey()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.body").value(body));
  }

  @Test
  void validatesBody() throws Exception {
    Object[] invalid = {
      Map.of("body", ""), Map.of("body", "    "), Map.of("body", "x".repeat(2001)), "{}"
    };
    for (Object b : invalid) {
      mvc.perform(api.post(alice, "/api/v1/tickets/{key}/comments", b, ticket.getKey()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
          .andExpect(jsonPath("$.errors[0].field").value("body"));
    }
    mvc.perform(
            api.post(
                alice,
                "/api/v1/tickets/{key}/comments",
                Map.of("body", "ok", "author", "x"),
                ticket.getKey()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("author"));
    mvc.perform(
            api.post(
                alice,
                "/api/v1/tickets/{key}/comments",
                Map.of("body", "x".repeat(2000)),
                ticket.getKey()))
        .andExpect(status().isCreated());
    assertThat(commentCount()).isEqualTo(1);
  }

  @Test
  void closedTicketRejectsComments() throws Exception {
    Ticket closed = fixtures.createInStatus(alice, TicketStatus.CLOSED);
    mvc.perform(
            api.post(
                alice,
                "/api/v1/tickets/{key}/comments",
                Map.of("body", "Too late"),
                closed.getKey()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TICKET_CLOSED"));
    assertThat(commentCount()).isZero();
  }

  @Test
  void unknownTicketIs404() throws Exception {
    mvc.perform(api.post(alice, "/api/v1/tickets/TMS-777777/comments", Map.of("body", "Hello")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    mvc.perform(api.get(alice, "/api/v1/tickets/TMS-777777/comments"))
        .andExpect(status().isNotFound());
  }
}
