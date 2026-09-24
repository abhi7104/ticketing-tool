package com.c2certi.tms.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c2certi.tms.support.IntegrationTest;
import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.user.domain.User;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TicketSummaryIntegrationTest extends IntegrationTest {

  private User alice;
  private User bob;

  @BeforeEach
  void setUp() {
    alice = users.create("alice");
    bob = users.create("bob");
  }

  private long summary(User user) throws Exception {
    String body =
        mvc.perform(api.get(user, "/api/v1/tickets/summary"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return Long.parseLong(body.replaceAll("\\D+", ""));
  }

  private long dbCount(User user) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM ticket WHERE assignee_id = ? AND status IN ('OPEN','IN_PROGRESS')",
        Long.class,
        user.getId());
  }

  @Test
  void zeroForNewUser() throws Exception {
    mvc.perform(api.get(bob, "/api/v1/tickets/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assignedPending").value(0));
  }

  @Test
  void countsOnlyMyPendingTicketsAndFollowsChanges() throws Exception {
    Ticket a = fixtures.create(alice, bob, "First ticket", "Assigned to bob");
    Ticket b = fixtures.create(alice, bob, "Second ticket", "Assigned to bob");
    fixtures.create(alice, bob, "Third ticket", "Assigned to bob");
    fixtures.create(bob, alice, "Alice's ticket", "Assigned to alice");
    assertThat(summary(bob)).isEqualTo(3).isEqualTo(dbCount(bob));

    // In progress still counts
    mvc.perform(
            api.post(
                    bob,
                    "/api/v1/tickets/{key}/transitions",
                    Map.of("targetStatus", "IN_PROGRESS"),
                    a.getKey())
                .header("If-Match", "\"0\""))
        .andExpect(status().isOk());
    assertThat(summary(bob)).isEqualTo(3);

    // Resolved no longer counts
    mvc.perform(
            api.post(
                    bob,
                    "/api/v1/tickets/{key}/transitions",
                    Map.of("targetStatus", "RESOLVED"),
                    a.getKey())
                .header("If-Match", "\"1\""))
        .andExpect(status().isOk());
    assertThat(summary(bob)).isEqualTo(2);

    // Reassigned away
    mvc.perform(
            api.patch(bob, "/api/v1/tickets/{key}", Map.of("assigneeId", alice.getId()), b.getKey())
                .header("If-Match", "\"0\""))
        .andExpect(status().isOk());
    assertThat(summary(bob)).isEqualTo(1).isEqualTo(dbCount(bob));
    assertThat(summary(alice)).isEqualTo(2).isEqualTo(dbCount(alice));

    // New ticket assigned to self
    mvc.perform(
            api.post(
                bob,
                "/api/v1/tickets",
                Map.of(
                    "title", "Self assigned",
                    "description", "Something I will do myself",
                    "priority", "LOW",
                    "assigneeId", bob.getId())))
        .andExpect(status().isCreated());
    assertThat(summary(bob)).isEqualTo(2).isEqualTo(dbCount(bob));
  }

  @Test
  void requiresSession() throws Exception {
    mvc.perform(get("/api/v1/tickets/summary"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }
}
