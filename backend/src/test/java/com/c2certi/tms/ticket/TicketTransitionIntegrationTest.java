package com.c2certi.tms.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c2certi.tms.support.ApiClient;
import com.c2certi.tms.support.IntegrationTest;
import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.ticket.domain.TicketStatus;
import com.c2certi.tms.user.domain.User;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

/**
 * State-machine integration tests: every (from, to) pair is sent through the real HTTP endpoint
 * against real PostgreSQL. Only OPEN→IN_PROGRESS→RESOLVED→CLOSED may succeed.
 */
class TicketTransitionIntegrationTest extends IntegrationTest {

  private static final Set<String> ALLOWED =
      Set.of("OPEN->IN_PROGRESS", "IN_PROGRESS->RESOLVED", "RESOLVED->CLOSED");

  private User alice;

  @BeforeEach
  void setUp() {
    alice = users.create("alice", "Alice Moore");
  }

  static Stream<Arguments> allPairs() {
    return EnumSet.allOf(TicketStatus.class).stream()
        .flatMap(
            from -> EnumSet.allOf(TicketStatus.class).stream().map(to -> Arguments.of(from, to)));
  }

  private record Snapshot(String status, long version, long historyCount, String updatedAt) {}

  private Snapshot snapshot(String key) {
    return jdbc.queryForObject(
        "SELECT t.status, t.version, t.updated_at::text AS updated,"
            + " (SELECT count(*) FROM ticket_history h WHERE h.ticket_id = t.id) AS hc"
            + " FROM ticket t WHERE t.ticket_key = ?",
        (rs, i) ->
            new Snapshot(
                rs.getString("status"),
                rs.getLong("version"),
                rs.getLong("hc"),
                rs.getString("updated")),
        key);
  }

  private org.springframework.test.web.servlet.ResultActions transition(
      String key, String ifMatch, Object target) throws Exception {
    var request =
        api.post(alice, "/api/v1/tickets/{key}/transitions", Map.of("targetStatus", target), key);
    if (ifMatch != null) {
      request.header("If-Match", ifMatch);
    }
    return mvc.perform(request);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @MethodSource("allPairs")
  void enforcesStateMachine(TicketStatus from, TicketStatus to) throws Exception {
    Ticket ticket = fixtures.createInStatus(alice, from);
    String key = ticket.getKey();
    Snapshot before = snapshot(key);

    if (ALLOWED.contains(from + "->" + to)) {
      transition(key, "\"" + before.version() + "\"", to.name())
          .andExpect(status().isOk())
          .andExpect(header().string("ETag", "\"" + (before.version() + 1) + "\""))
          .andExpect(jsonPath("$.status").value(to.name()))
          .andExpect(jsonPath("$.history[-1:].changeType").value("STATUS_CHANGED"))
          .andExpect(jsonPath("$.history[-1:].oldValue").value(from.name()))
          .andExpect(jsonPath("$.history[-1:].newValue").value(to.name()))
          .andExpect(jsonPath("$.history[-1:].actor.displayName").value("Alice Moore"));

      Snapshot after = snapshot(key);
      assertThat(after.status()).isEqualTo(to.name());
      assertThat(after.version()).isEqualTo(before.version() + 1);
      assertThat(after.historyCount()).isEqualTo(before.historyCount() + 1);
      String stamps =
          jdbc.queryForObject(
              "SELECT coalesce(resolved_at::text,'-') || '|' || coalesce(closed_at::text,'-')"
                  + " FROM ticket WHERE ticket_key = ?",
              String.class,
              key);
      if (to == TicketStatus.RESOLVED) {
        assertThat(stamps).doesNotStartWith("-|");
      }
      if (to == TicketStatus.CLOSED) {
        assertThat(stamps).doesNotEndWith("|-");
      }
    } else {
      transition(key, "\"" + before.version() + "\"", to.name())
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"))
          .andExpect(jsonPath("$.title").value("Status change not allowed"))
          .andExpect(jsonPath("$.currentStatus").value(from.name()))
          .andExpect(
              from == TicketStatus.CLOSED
                  ? jsonPath("$.allowedNextStatus").doesNotExist()
                  : jsonPath("$.allowedNextStatus").value(from.allowedNext().orElseThrow().name()));

      assertThat(snapshot(key)).isEqualTo(before);
    }
  }

  @Test
  void fullValidFlowSucceedsStepByStep() throws Exception {
    String key = fixtures.create(alice).getKey();
    long version = 0;
    for (TicketStatus target :
        List.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED, TicketStatus.CLOSED)) {
      transition(key, "\"" + version + "\"", target.name()).andExpect(status().isOk());
      version++;
    }
    assertThat(snapshot(key).status()).isEqualTo("CLOSED");
    assertThat(snapshot(key).historyCount()).isEqualTo(4);
  }

  @Test
  void missingIfMatchIs428() throws Exception {
    String key = fixtures.create(alice).getKey();
    transition(key, null, "IN_PROGRESS")
        .andExpect(status().isPreconditionRequired())
        .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
    assertThat(snapshot(key).status()).isEqualTo("OPEN");
  }

  @Test
  void staleIfMatchIs412() throws Exception {
    String key = fixtures.create(alice).getKey();
    transition(key, "\"7\"", "IN_PROGRESS")
        .andExpect(status().isPreconditionFailed())
        .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    assertThat(snapshot(key).status()).isEqualTo("OPEN");
  }

  @Test
  void unknownTicketIs404() throws Exception {
    transition("TMS-424242", "\"0\"", "IN_PROGRESS")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
  }

  @Test
  void invalidOrMissingTargetIs400() throws Exception {
    String key = fixtures.create(alice).getKey();
    transition(key, "\"0\"", "DONE")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("targetStatus"));
    mvc.perform(
            api.post(alice, "/api/v1/tickets/{key}/transitions", "{}", key)
                .header("If-Match", "\"0\""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("targetStatus"));
    assertThat(snapshot(key).status()).isEqualTo("OPEN");
  }

  @Test
  void statusCannotBeChangedThroughFieldUpdate() throws Exception {
    String key = fixtures.create(alice).getKey();
    mvc.perform(
            api.patch(alice, "/api/v1/tickets/{key}", Map.of("status", "CLOSED"), key)
                .header("If-Match", "\"0\""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("status"));
    assertThat(snapshot(key).status()).isEqualTo("OPEN");
  }

  @Test
  void requiresSession() throws Exception {
    String key = fixtures.create(alice).getKey();
    mvc.perform(
            post("/api/v1/tickets/{key}/transitions", key)
                .with(ApiClient.csrf())
                .header("If-Match", "\"0\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetStatus\":\"IN_PROGRESS\"}"))
        .andExpect(status().isUnauthorized());
    assertThat(snapshot(key).status()).isEqualTo("OPEN");
  }

  @Test
  void concurrentTransitionsWithSameVersionOnlyOneWins() throws Exception {
    String key = fixtures.create(alice).getKey();
    int parallel = 4;
    ExecutorService pool = Executors.newFixedThreadPool(parallel);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Integer>> results = new ArrayList<>();
    for (int i = 0; i < parallel; i++) {
      Callable<Integer> call =
          () -> {
            start.await();
            return transition(key, "\"0\"", "IN_PROGRESS").andReturn().getResponse().getStatus();
          };
      results.add(pool.submit(call));
    }
    start.countDown();
    List<Integer> statuses = new ArrayList<>();
    for (Future<Integer> f : results) {
      statuses.add(f.get());
    }
    pool.shutdown();

    assertThat(statuses).filteredOn(s -> s == 200).hasSize(1);
    assertThat(statuses).filteredOn(s -> s != 200).allMatch(s -> s == 412 || s == 409);
    Snapshot after = snapshot(key);
    assertThat(after.status()).isEqualTo("IN_PROGRESS");
    assertThat(after.version()).isEqualTo(1);
    assertThat(after.historyCount()).isEqualTo(2);
  }
}
