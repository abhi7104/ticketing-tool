package com.c2certi.tms.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c2certi.tms.support.IntegrationTest;
import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.ticket.domain.TicketStatus;
import com.c2certi.tms.user.domain.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class TicketUpdateIntegrationTest extends IntegrationTest {

  private User alice;
  private User bob;
  private Ticket ticket;

  @BeforeEach
  void setUp() {
    alice = users.create("alice", "Alice Moore");
    bob = users.create("bob", "Bob Singh");
    ticket = fixtures.create(alice, alice, "Printer jam", "Paper stuck in tray two");
  }

  private ResultActions patch(Object body, String ifMatch) throws Exception {
    var request = api.patch(alice, "/api/v1/tickets/{key}", body, ticket.getKey());
    if (ifMatch != null) {
      request.header("If-Match", ifMatch);
    }
    return mvc.perform(request);
  }

  private List<Map<String, Object>> fieldHistory() {
    return jdbc.queryForList(
        "SELECT h.field_name, h.old_value, h.new_value FROM ticket_history h"
            + " JOIN ticket t ON t.id = h.ticket_id"
            + " WHERE t.ticket_key = ? AND h.change_type = 'FIELD_UPDATED' ORDER BY h.id",
        ticket.getKey());
  }

  @Test
  void updatesEachFieldAndRecordsHistory() throws Exception {
    patch(Map.of("title", "Printer jam on floor 2"), "\"0\"")
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"1\""))
        .andExpect(jsonPath("$.title").value("Printer jam on floor 2"))
        .andExpect(jsonPath("$.description").value("Paper stuck in tray two"));
    patch(Map.of("description", "Paper stuck in tray two and three"), "\"1\"")
        .andExpect(jsonPath("$.description").value("Paper stuck in tray two and three"));
    patch(Map.of("priority", "CRITICAL"), "\"2\"")
        .andExpect(jsonPath("$.priority").value("CRITICAL"));
    patch(Map.of("assigneeId", bob.getId()), "\"3\"")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assignee.displayName").value("Bob Singh"))
        .andExpect(jsonPath("$.version").value(4));

    assertThat(fieldHistory())
        .extracting(r -> r.get("field_name") + ":" + r.get("old_value") + "->" + r.get("new_value"))
        .containsExactly(
            "title:Printer jam->Printer jam on floor 2",
            "description:Paper stuck in tray two->Paper stuck in tray two and three",
            "priority:MEDIUM->CRITICAL",
            "assignee:Alice Moore->Bob Singh");

    mvc.perform(api.get(alice, "/api/v1/tickets?view=all"))
        .andExpect(jsonPath("$.items[0].assignee.displayName").value("Bob Singh"));
  }

  @Test
  void multiFieldUpdateWritesOneRowPerChangedFieldOnly() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("title", "Printer jam");
    body.put("priority", "HIGH");
    body.put("assigneeId", bob.getId());
    patch(body, "\"0\"").andExpect(status().isOk());
    assertThat(fieldHistory())
        .extracting(r -> r.get("field_name"))
        .containsExactly("priority", "assignee");
  }

  @Test
  void updateBumpsUpdatedAt() throws Exception {
    jdbc.update(
        "UPDATE ticket SET updated_at = now() - interval '1 day' WHERE ticket_key = ?",
        ticket.getKey());
    String before =
        jdbc.queryForObject(
            "SELECT updated_at::text FROM ticket WHERE ticket_key = ?",
            String.class,
            ticket.getKey());
    patch(Map.of("priority", "LOW"), "\"0\"").andExpect(status().isOk());
    String after =
        jdbc.queryForObject(
            "SELECT updated_at::text FROM ticket WHERE ticket_key = ?",
            String.class,
            ticket.getKey());
    assertThat(after).isNotEqualTo(before);
  }

  @Test
  void validatesLikeCreate() throws Exception {
    Object[][] cases = {
      {Map.of("title", "  "), "title"},
      {Map.of("title", "ab"), "title"},
      {Map.of("title", "x".repeat(151)), "title"},
      {Map.of("description", "short"), "description"},
      {Map.of("description", "x".repeat(5001)), "description"},
      {Map.of("priority", "URGENT"), "priority"},
      {Map.of("assigneeId", 987654), "assigneeId"},
      {Map.of("status", "CLOSED"), "status"},
      {Map.of("reporterId", 1), "reporterId"},
    };
    for (Object[] c : cases) {
      patch(c[0], "\"0\"")
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
          .andExpect(jsonPath("$.errors[*].field", hasItem(c[1])));
    }
    patch("{}", "\"0\"")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors", hasSize(1)));
    assertThat(fieldHistory()).isEmpty();
    assertThat(
            jdbc.queryForObject(
                "SELECT version FROM ticket WHERE ticket_key = ?", Long.class, ticket.getKey()))
        .isZero();
  }

  @Test
  void deactivatedAssigneeIsRejected() throws Exception {
    User gone = users.deactivated("gone");
    patch(Map.of("assigneeId", gone.getId()), "\"0\"")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("assigneeId"));
  }

  @Test
  void closedTicketIsReadOnly() throws Exception {
    Ticket closed = fixtures.createInStatus(alice, TicketStatus.CLOSED);
    mvc.perform(
            api.patch(alice, "/api/v1/tickets/{key}", Map.of("title", "New title"), closed.getKey())
                .header("If-Match", "\"" + closed.getVersion() + "\""))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TICKET_CLOSED"))
        .andExpect(jsonPath("$.detail").value("Closed tickets can't be changed."));
  }

  @Test
  void staleVersionIs412AndMissingIs428() throws Exception {
    patch(Map.of("title", "First edit"), "\"0\"").andExpect(status().isOk());
    patch(Map.of("title", "Second edit from stale tab"), "\"0\"")
        .andExpect(status().isPreconditionFailed())
        .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"))
        .andExpect(jsonPath("$.title").value("Ticket was updated by someone else"));
    patch(Map.of("title", "No version"), null)
        .andExpect(status().isPreconditionRequired())
        .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
    assertThat(
            jdbc.queryForObject(
                "SELECT title FROM ticket WHERE ticket_key = ?", String.class, ticket.getKey()))
        .isEqualTo("First edit");
  }

  @Test
  void unknownTicketIs404() throws Exception {
    mvc.perform(
            api.patch(alice, "/api/v1/tickets/TMS-99999", Map.of("title", "Whatever"))
                .header("If-Match", "\"0\""))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
  }
}
