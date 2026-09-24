package com.c2certi.tms.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c2certi.tms.support.IntegrationTest;
import com.c2certi.tms.user.domain.User;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** SC-002: search + filter over 10,000 tickets returns in under one second. Run with -Pperf. */
@Tag("perf")
class TicketSearchPerformanceTest extends IntegrationTest {

  @Test
  void searchOverTenThousandTicketsIsFast() throws Exception {
    User alice = users.create("alice");
    jdbc.update(
        "INSERT INTO ticket (ticket_key, title, description, priority, status, reporter_id,"
            + " assignee_id, created_at, updated_at)"
            + " SELECT 'TMS-' || nextval('ticket_number_seq'), 'Issue number ' || g,"
            + " 'Generated description for load testing ' || md5(g::text),"
            + " (ARRAY['LOW','MEDIUM','HIGH','CRITICAL'])[1 + g % 4],"
            + " (ARRAY['OPEN','IN_PROGRESS','RESOLVED','CLOSED'])[1 + g % 4],"
            + " ?, ?, now() - (g || ' minutes')::interval, now() - (g || ' minutes')::interval"
            + " FROM generate_series(1, 10000) g",
        alice.getId(), alice.getId());
    jdbc.execute("ANALYZE ticket");

    // warm-up
    mvc.perform(api.get(alice, "/api/v1/tickets").param("view", "all").param("q", "issue"));

    long worst = 0;
    for (String q : new String[] {"number 99", "a1b", "TMS-50", "load testing"}) {
      long start = System.nanoTime();
      mvc.perform(
              api.get(alice, "/api/v1/tickets")
                  .param("view", "all")
                  .param("status", "OPEN")
                  .param("q", q))
          .andExpect(status().isOk());
      worst = Math.max(worst, (System.nanoTime() - start) / 1_000_000);
    }
    assertThat(worst).as("slowest search in ms").isLessThan(1000);
  }

  /** SC-002 (feature 002): 500 of 10,000 tickets assigned to one user. */
  @Test
  void assignedQueueAndCountAreFast() throws Exception {
    User bob = users.create("bob");
    User alice = users.create("alice");
    jdbc.update(
        "INSERT INTO ticket (ticket_key, title, description, priority, status, reporter_id,"
            + " assignee_id, created_at, updated_at)"
            + " SELECT 'TMS-' || nextval('ticket_number_seq'), 'Queue item ' || g,"
            + " 'Generated description for load testing ' || md5(g::text),"
            + " (ARRAY['LOW','MEDIUM','HIGH','CRITICAL'])[1 + g % 4],"
            + " (ARRAY['OPEN','IN_PROGRESS','RESOLVED','CLOSED'])[1 + (g / 4) % 4],"
            + " ?, CASE WHEN g % 20 = 0 THEN ? ELSE ? END,"
            + " now() - (g || ' minutes')::interval, now() - (g || ' minutes')::interval"
            + " FROM generate_series(1, 10000) g",
        alice.getId(), bob.getId(), alice.getId());
    jdbc.execute("ANALYZE ticket");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM ticket WHERE assignee_id = ?", Long.class, bob.getId()))
        .isEqualTo(500);

    mvc.perform(api.get(bob, "/api/v1/tickets?view=assigned")); // warm-up
    long worst = 0;
    for (String url :
        new String[] {
          "/api/v1/tickets?view=assigned",
          "/api/v1/tickets?view=assigned&q=queue",
          "/api/v1/tickets?view=assigned&status=IN_PROGRESS&q=load",
          "/api/v1/tickets/summary"
        }) {
      long start = System.nanoTime();
      mvc.perform(api.get(bob, url)).andExpect(status().isOk());
      worst = Math.max(worst, (System.nanoTime() - start) / 1_000_000);
    }
    assertThat(worst).as("slowest assigned-queue request in ms").isLessThan(1000);
  }
}
