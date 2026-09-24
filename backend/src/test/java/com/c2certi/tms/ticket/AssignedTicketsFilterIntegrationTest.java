package com.c2certi.tms.ticket;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c2certi.tms.support.IntegrationTest;
import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Search and status filter inside "Assigned to me". */
class AssignedTicketsFilterIntegrationTest extends IntegrationTest {

  private User alice;
  private User bob;
  private Ticket printer;
  private Ticket vpn;
  private Ticket resolved;

  @BeforeEach
  void setUp() {
    alice = users.create("alice");
    bob = users.create("bob");
    printer = fixtures.create(alice, bob, "Printer jam", "Paper stuck in tray two");
    vpn = fixtures.create(alice, bob, "VPN drops", "Remote access at 100% failure");
    jdbc.update("UPDATE ticket SET status = 'IN_PROGRESS' WHERE id = ?", vpn.getId());
    resolved = fixtures.create(alice, bob, "Printer toner", "Toner replaced already");
    jdbc.update("UPDATE ticket SET status = 'RESOLVED' WHERE id = ?", resolved.getId());
    fixtures.create(bob, alice, "Printer on floor 3", "Assigned to alice, not bob");
  }

  private MockHttpServletRequestBuilder assigned() {
    return api.get(bob, "/api/v1/tickets").param("view", "assigned");
  }

  @Test
  void keywordMatchesTitleDescriptionAndKeyWithinMyQueue() throws Exception {
    mvc.perform(assigned().param("q", "PRINTER"))
        .andExpect(jsonPath("$.items[*].key", contains(printer.getKey())));
    mvc.perform(assigned().param("q", "remote access"))
        .andExpect(jsonPath("$.items[*].key", contains(vpn.getKey())));
    mvc.perform(assigned().param("q", vpn.getKey().toLowerCase()))
        .andExpect(jsonPath("$.items[*].key", contains(vpn.getKey())));
    mvc.perform(assigned().param("q", "100%"))
        .andExpect(jsonPath("$.items[*].key", contains(vpn.getKey())));
    mvc.perform(assigned().param("q", "_")).andExpect(jsonPath("$.items", hasSize(0)));
  }

  @Test
  void statusFilterNarrowsAndFinishedStatusesAreEmpty() throws Exception {
    mvc.perform(assigned().param("status", "OPEN"))
        .andExpect(jsonPath("$.items[*].key", contains(printer.getKey())));
    mvc.perform(assigned().param("status", "IN_PROGRESS"))
        .andExpect(jsonPath("$.items[*].key", contains(vpn.getKey())));
    for (String finished : new String[] {"RESOLVED", "CLOSED"}) {
      mvc.perform(assigned().param("status", finished))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items", hasSize(0)))
          .andExpect(jsonPath("$.totalItems").value(0));
    }
  }

  @Test
  void keywordAndStatusCombine() throws Exception {
    mvc.perform(assigned().param("q", "printer").param("status", "IN_PROGRESS"))
        .andExpect(jsonPath("$.items", hasSize(0)));
    mvc.perform(assigned().param("q", "printer").param("status", "OPEN"))
        .andExpect(jsonPath("$.items[*].key", contains(printer.getKey())));
  }

  @Test
  void pagingInsideTheView() throws Exception {
    mvc.perform(assigned().param("size", "1").param("page", "0"))
        .andExpect(jsonPath("$.totalItems").value(2))
        .andExpect(jsonPath("$.totalPages").value(2));
    mvc.perform(assigned().param("size", "1").param("page", "1"))
        .andExpect(jsonPath("$.items", hasSize(1)));
    mvc.perform(assigned())
        .andExpect(jsonPath("$.items[*].key", containsInAnyOrder(printer.getKey(), vpn.getKey())));
  }
}
