package com.c2certi.tms.ticket;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c2certi.tms.support.IntegrationTest;
import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.ticket.domain.TicketStatus;
import com.c2certi.tms.user.domain.User;
import org.junit.jupiter.api.Test;

class TicketDetailIntegrationTest extends IntegrationTest {

  @Test
  void returnsDetailWithEtagAndAllowedNextStatus() throws Exception {
    User alice = users.create("alice", "Alice Moore");
    User bob = users.create("bob", "Bob Singh");
    Ticket ticket = fixtures.create(alice, bob, "VPN drops", "VPN disconnects every 10 minutes");

    mvc.perform(api.get(alice, "/api/v1/tickets/{key}", ticket.getKey()))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"0\""))
        .andExpect(jsonPath("$.key").value(ticket.getKey()))
        .andExpect(jsonPath("$.title").value("VPN drops"))
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.priority").value("MEDIUM"))
        .andExpect(jsonPath("$.reporter.displayName").value("Alice Moore"))
        .andExpect(jsonPath("$.assignee.displayName").value("Bob Singh"))
        .andExpect(jsonPath("$.version").value(0))
        .andExpect(jsonPath("$.allowedNextStatus").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.comments", hasSize(0)))
        .andExpect(jsonPath("$.history", hasSize(1)))
        .andExpect(jsonPath("$.history[0].changeType").value("CREATED"));
  }

  @Test
  void closedTicketHasNoAllowedNextStatus() throws Exception {
    User alice = users.create("alice");
    Ticket ticket = fixtures.createInStatus(alice, TicketStatus.CLOSED);

    mvc.perform(api.get(alice, "/api/v1/tickets/{key}", ticket.getKey()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"))
        .andExpect(jsonPath("$.allowedNextStatus").doesNotExist())
        .andExpect(jsonPath("$.closedAt").isNotEmpty())
        .andExpect(jsonPath("$.resolvedAt").isNotEmpty());
  }

  @Test
  void unknownTicketGivesFriendlyNotFound() throws Exception {
    User alice = users.create("alice");
    mvc.perform(api.get(alice, "/api/v1/tickets/TMS-9999"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"))
        .andExpect(jsonPath("$.title").value("Ticket not found"))
        .andExpect(jsonPath("$.detail").value("We couldn't find ticket TMS-9999."))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(content().string(not(containsString("Exception"))));
  }

  @Test
  void malformedKeyIsValidationError() throws Exception {
    User alice = users.create("alice");
    mvc.perform(api.get(alice, "/api/v1/tickets/not-a-key"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors[0].field").value("ticketKey"));
  }
}
