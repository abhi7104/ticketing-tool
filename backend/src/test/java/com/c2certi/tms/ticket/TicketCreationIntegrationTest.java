package com.c2certi.tms.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c2certi.tms.support.ApiClient;
import com.c2certi.tms.support.IntegrationTest;
import com.c2certi.tms.user.domain.User;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

class TicketCreationIntegrationTest extends IntegrationTest {

  private User alice;
  private User bob;

  @BeforeEach
  void setUp() {
    alice = users.create("alice", "Alice Moore");
    bob = users.create("bob", "Bob Singh");
  }

  private Map<String, Object> validBody() {
    Map<String, Object> body = new HashMap<>();
    body.put("title", "Laptop won't boot");
    body.put("description", "Black screen after the latest update.");
    body.put("priority", "HIGH");
    body.put("assigneeId", bob.getId());
    return body;
  }

  private long ticketCount() {
    return jdbc.queryForObject("SELECT count(*) FROM ticket", Long.class);
  }

  @Test
  void createsOpenTicketWithKeyReporterAndHistory() throws Exception {
    Map<String, Object> body = validBody();
    body.put("title", "  Laptop won't boot  ");

    mvc.perform(api.post(alice, "/api/v1/tickets", body))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/v1/tickets/TMS-\\d+")))
        .andExpect(header().string("ETag", "\"0\""))
        .andExpect(jsonPath("$.key").value(matchesPattern("TMS-\\d+")))
        .andExpect(jsonPath("$.title").value("Laptop won't boot"))
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.priority").value("HIGH"))
        .andExpect(jsonPath("$.reporter.id").value(alice.getId()))
        .andExpect(jsonPath("$.assignee.id").value(bob.getId()))
        .andExpect(jsonPath("$.allowedNextStatus").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.history", hasSize(1)))
        .andExpect(jsonPath("$.history[0].changeType").value("CREATED"))
        .andExpect(jsonPath("$.history[0].actor.id").value(alice.getId()));

    assertThat(ticketCount()).isEqualTo(1);
  }

  @Test
  void keysAreUnique() throws Exception {
    String first =
        mvc.perform(api.post(alice, "/api/v1/tickets", validBody()))
            .andReturn()
            .getResponse()
            .getHeader("Location");
    String second =
        mvc.perform(api.post(alice, "/api/v1/tickets", validBody()))
            .andReturn()
            .getResponse()
            .getHeader("Location");
    assertThat(first).isNotEqualTo(second);
  }

  static Stream<Arguments> invalidBodies() {
    return Stream.of(
        Arguments.of("title", null),
        Arguments.of("title", "   "),
        Arguments.of("title", "ab"),
        Arguments.of("title", "x".repeat(151)),
        Arguments.of("description", null),
        Arguments.of("description", "too short"),
        Arguments.of("description", "x".repeat(5001)),
        Arguments.of("priority", null),
        Arguments.of("priority", "URGENT"),
        Arguments.of("assigneeId", null),
        Arguments.of("assigneeId", 999999));
  }

  @ParameterizedTest(name = "{0}={1}")
  @MethodSource("invalidBodies")
  void rejectsInvalidField(String field, Object value) throws Exception {
    Map<String, Object> body = validBody();
    body.put(field, value);

    mvc.perform(api.post(alice, "/api/v1/tickets", body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors[*].field", hasItem(field)));

    assertThat(ticketCount()).isZero();
  }

  @Test
  void boundaryLengthsAreAccepted() throws Exception {
    Map<String, Object> body = validBody();
    body.put("title", "abc");
    body.put("description", "x".repeat(10));
    mvc.perform(api.post(alice, "/api/v1/tickets", body)).andExpect(status().isCreated());
    body.put("title", "t".repeat(150));
    body.put("description", "d".repeat(5000));
    mvc.perform(api.post(alice, "/api/v1/tickets", body)).andExpect(status().isCreated());
  }

  @Test
  void emptyBodyReportsAllFourFields() throws Exception {
    mvc.perform(api.post(alice, "/api/v1/tickets", "{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors", hasSize(4)))
        .andExpect(jsonPath("$.errors[?(@.field=='title')].message").value("Title is required."));
  }

  @Test
  void reportsEveryInvalidFieldAtOnceEvenWithBadPriority() throws Exception {
    mvc.perform(
            api.post(
                alice,
                "/api/v1/tickets",
                Map.of("title", "x", "description", "short", "priority", "URGENT")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors", hasSize(4)))
        .andExpect(
            jsonPath("$.errors[?(@.field=='priority')].message")
                .value("Priority must be one of LOW, MEDIUM, HIGH, CRITICAL."));
  }

  @Test
  void rejectsInactiveAssignee() throws Exception {
    User gone = users.deactivated("gone");
    Map<String, Object> body = validBody();
    body.put("assigneeId", gone.getId());
    mvc.perform(api.post(alice, "/api/v1/tickets", body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("assigneeId"));
  }

  @Test
  void rejectsClientSuppliedStatus() throws Exception {
    Map<String, Object> body = validBody();
    body.put("status", "CLOSED");
    mvc.perform(api.post(alice, "/api/v1/tickets", body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("status"));
    assertThat(ticketCount()).isZero();
  }

  @Test
  void rejectsMalformedJson() throws Exception {
    mvc.perform(api.post(alice, "/api/v1/tickets", "{not json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  void requiresSession() throws Exception {
    mvc.perform(
            post("/api/v1/tickets")
                .with(ApiClient.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(api.toJson(validBody())))
        .andExpect(status().isUnauthorized());
    assertThat(ticketCount()).isZero();
  }
}
