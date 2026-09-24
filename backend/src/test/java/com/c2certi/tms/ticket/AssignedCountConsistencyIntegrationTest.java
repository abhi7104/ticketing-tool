package com.c2certi.tms.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import com.c2certi.tms.support.IntegrationTest;
import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.ticket.domain.TicketStatus;
import com.c2certi.tms.user.domain.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** SC-003/SC-004: the badge count always equals the unfiltered "Assigned to me" total. */
class AssignedCountConsistencyIntegrationTest extends IntegrationTest {

  @Autowired ObjectMapper json;

  private JsonNode getJson(User user, String url) throws Exception {
    return json.readTree(
        mvc.perform(api.get(user, url)).andReturn().getResponse().getContentAsString());
  }

  private void assertConsistent(List<User> people) throws Exception {
    for (User u : people) {
      long count = getJson(u, "/api/v1/tickets/summary").get("assignedPending").asLong();
      long total = getJson(u, "/api/v1/tickets?view=assigned&size=1").get("totalItems").asLong();
      long db =
          jdbc.queryForObject(
              "SELECT count(*) FROM ticket WHERE assignee_id = ? AND status IN ('OPEN','IN_PROGRESS')",
              Long.class,
              u.getId());
      assertThat(count).as("count for %s", u.getUsername()).isEqualTo(total).isEqualTo(db);
    }
  }

  @Test
  void countMatchesViewForRandomDataBeforeAndAfterChanges() throws Exception {
    List<User> people = List.of(users.create("alice"), users.create("bob"), users.create("carol"));
    Random random = new Random(42);
    TicketStatus[] statuses = TicketStatus.values();
    List<Ticket> created = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      User reporter = people.get(random.nextInt(3));
      User assignee = people.get(random.nextInt(3));
      Ticket t =
          fixtures.create(reporter, assignee, "Random ticket " + i, "Generated for consistency");
      jdbc.update(
          "UPDATE ticket SET status = ? WHERE id = ?",
          statuses[random.nextInt(statuses.length)].name(),
          t.getId());
      created.add(t);
    }
    assertConsistent(people);

    for (int i = 0; i < 20; i++) {
      Ticket t = created.get(random.nextInt(created.size()));
      if (random.nextBoolean()) {
        jdbc.update(
            "UPDATE ticket SET assignee_id = ? WHERE id = ?",
            people.get(random.nextInt(3)).getId(),
            t.getId());
      } else {
        jdbc.update(
            "UPDATE ticket SET status = ? WHERE id = ?",
            statuses[random.nextInt(statuses.length)].name(),
            t.getId());
      }
    }
    assertConsistent(people);
  }
}
