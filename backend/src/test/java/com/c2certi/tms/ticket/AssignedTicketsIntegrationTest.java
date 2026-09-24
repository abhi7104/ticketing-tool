package com.c2certi.tms.ticket;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c2certi.tms.support.IntegrationTest;
import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.ticket.domain.TicketStatus;
import com.c2certi.tms.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** "Assigned to me": only the signed-in user's Open/In progress tickets, most urgent first. */
class AssignedTicketsIntegrationTest extends IntegrationTest {

  private User alice;
  private User bob;

  @BeforeEach
  void setUp() {
    alice = users.create("alice");
    bob = users.create("bob");
  }

  private Ticket ticket(User reporter, User assignee, String priority, int ageMinutes) {
    Ticket t =
        fixtures.create(
            reporter, assignee, "Ticket " + priority + " " + ageMinutes, "Some description");
    jdbc.update(
        "UPDATE ticket SET priority = ?, created_at = now() - (? || ' minutes')::interval WHERE id = ?",
        priority,
        String.valueOf(ageMinutes),
        t.getId());
    return t;
  }

  private void setStatus(Ticket t, TicketStatus status) {
    jdbc.update("UPDATE ticket SET status = ? WHERE id = ?", status.name(), t.getId());
  }

  @Test
  void showsOnlyMyPendingTicketsMostUrgentFirst() throws Exception {
    Ticket low = ticket(alice, bob, "LOW", 50);
    Ticket critical = ticket(alice, bob, "CRITICAL", 10);
    Ticket highOld = ticket(alice, bob, "HIGH", 40);
    Ticket highNew = ticket(alice, bob, "HIGH", 5);
    setStatus(highNew, TicketStatus.IN_PROGRESS);
    Ticket resolved = ticket(alice, bob, "CRITICAL", 60);
    setStatus(resolved, TicketStatus.RESOLVED);
    Ticket closed = ticket(alice, bob, "CRITICAL", 70);
    setStatus(closed, TicketStatus.CLOSED);
    ticket(bob, alice, "CRITICAL", 80); // assigned to someone else
    Ticket selfAssigned = ticket(bob, bob, "MEDIUM", 30);

    mvc.perform(api.get(bob, "/api/v1/tickets?view=assigned"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$.items[*].key",
                contains(
                    critical.getKey(),
                    highOld.getKey(),
                    highNew.getKey(),
                    selfAssigned.getKey(),
                    low.getKey())))
        .andExpect(jsonPath("$.totalItems").value(5))
        .andExpect(jsonPath("$.items[0].assignee.id").value(bob.getId()));
  }

  @Test
  void tiesAreBrokenByReferenceAndStableAcrossPages() throws Exception {
    jdbc.update("UPDATE ticket SET created_at = created_at"); // no-op, keeps fixture style
    Ticket first = ticket(alice, bob, "HIGH", 20);
    Ticket second = ticket(alice, bob, "HIGH", 20);
    Ticket third = ticket(alice, bob, "HIGH", 20);
    jdbc.update(
        "UPDATE ticket SET created_at = (SELECT created_at FROM ticket WHERE id = ?)",
        first.getId());

    mvc.perform(api.get(bob, "/api/v1/tickets?view=assigned&size=2&page=0"))
        .andExpect(jsonPath("$.items[*].key", contains(first.getKey(), second.getKey())))
        .andExpect(jsonPath("$.totalPages").value(2));
    mvc.perform(api.get(bob, "/api/v1/tickets?view=assigned&size=2&page=1"))
        .andExpect(jsonPath("$.items[*].key", contains(third.getKey())));
  }

  @Test
  void otherViewsKeepMostRecentlyUpdatedOrder() throws Exception {
    Ticket older = ticket(bob, bob, "CRITICAL", 10);
    Ticket newer = ticket(bob, bob, "LOW", 5);
    jdbc.update(
        "UPDATE ticket SET updated_at = now() - interval '1 hour' WHERE id = ?", older.getId());
    mvc.perform(api.get(bob, "/api/v1/tickets"))
        .andExpect(jsonPath("$.items[*].key", contains(newer.getKey(), older.getKey())));
    mvc.perform(api.get(bob, "/api/v1/tickets?view=all"))
        .andExpect(jsonPath("$.items[*].key", contains(newer.getKey(), older.getKey())));
  }

  @Test
  void emptyQueue() throws Exception {
    ticket(bob, alice, "HIGH", 5);
    mvc.perform(api.get(bob, "/api/v1/tickets?view=assigned"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(0)))
        .andExpect(jsonPath("$.totalItems").value(0));
  }

  @Test
  void rejectsUnknownView() throws Exception {
    mvc.perform(api.get(bob, "/api/v1/tickets?view=bogus"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors[0].field").value("view"));
  }

  @Test
  void requiresSession() throws Exception {
    mvc.perform(get("/api/v1/tickets?view=assigned")).andExpect(status().isUnauthorized());
  }
}
