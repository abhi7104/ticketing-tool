package com.c2certi.tms.ticket;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c2certi.tms.support.IntegrationTest;
import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.ticket.domain.TicketStatus;
import com.c2certi.tms.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TicketListIntegrationTest extends IntegrationTest {

  private User alice;
  private User bob;
  private Ticket printer;
  private Ticket vpn;
  private Ticket bobsTicket;

  @BeforeEach
  void setUp() {
    alice = users.create("alice");
    bob = users.create("bob");
    printer = fixtures.create(alice, bob, "Printer jam", "Paper stuck in tray two of the printer");
    vpn = fixtures.createInStatus(alice, TicketStatus.IN_PROGRESS);
    jdbc.update(
        "UPDATE ticket SET title = 'VPN drops hourly', description = 'Remote access keeps"
            + " failing' WHERE id = ?",
        vpn.getId());
    bobsTicket = fixtures.create(bob, alice, "Email quota", "Mailbox is 100% full, please help");
    // deterministic ordering: printer newest, then bob's, then vpn
    jdbc.update(
        "UPDATE ticket SET updated_at = now() - interval '3 hours' WHERE id = ?", vpn.getId());
    jdbc.update(
        "UPDATE ticket SET updated_at = now() - interval '2 hours' WHERE id = ?",
        bobsTicket.getId());
    jdbc.update(
        "UPDATE ticket SET updated_at = now() - interval '1 hours' WHERE id = ?", printer.getId());
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder all(String q) {
    return api.get(alice, "/api/v1/tickets").param("view", "all").param("q", q);
  }

  @Test
  void defaultsToMyTicketsNewestFirst() throws Exception {
    mvc.perform(api.get(alice, "/api/v1/tickets"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[*].key", contains(printer.getKey(), vpn.getKey())))
        .andExpect(jsonPath("$.items[0].title").value("Printer jam"))
        .andExpect(jsonPath("$.items[0].status").value("OPEN"))
        .andExpect(jsonPath("$.items[0].priority").value("MEDIUM"))
        .andExpect(jsonPath("$.items[0].assignee.id").value(bob.getId()))
        .andExpect(jsonPath("$.items[0].reporter.id").value(alice.getId()))
        .andExpect(jsonPath("$.items[0].updatedAt").isNotEmpty())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalItems").value(2))
        .andExpect(jsonPath("$.totalPages").value(1));
  }

  @Test
  void allViewIncludesOtherReporters() throws Exception {
    mvc.perform(api.get(alice, "/api/v1/tickets?view=all"))
        .andExpect(
            jsonPath(
                "$.items[*].key", contains(printer.getKey(), bobsTicket.getKey(), vpn.getKey())));
  }

  @Test
  void searchMatchesTitleDescriptionAndKeyCaseInsensitively() throws Exception {
    mvc.perform(all("PRINTER")).andExpect(jsonPath("$.items[*].key", contains(printer.getKey())));
    mvc.perform(all("remote access")).andExpect(jsonPath("$.items[*].key", contains(vpn.getKey())));
    mvc.perform(all(bobsTicket.getKey().toLowerCase()))
        .andExpect(jsonPath("$.items[*].key", contains(bobsTicket.getKey())));
  }

  @Test
  void wildcardCharactersAreLiteral() throws Exception {
    mvc.perform(all("100%")).andExpect(jsonPath("$.items[*].key", contains(bobsTicket.getKey())));
    mvc.perform(all("%%")).andExpect(jsonPath("$.items", hasSize(0)));
    mvc.perform(all("_")).andExpect(jsonPath("$.items", hasSize(0)));
    mvc.perform(all("\\")).andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)));
  }

  @Test
  void statusFilterAndCombinations() throws Exception {
    mvc.perform(api.get(alice, "/api/v1/tickets?view=all&status=OPEN"))
        .andExpect(
            jsonPath("$.items[*].key", containsInAnyOrder(printer.getKey(), bobsTicket.getKey())));
    mvc.perform(api.get(alice, "/api/v1/tickets?view=all&status=IN_PROGRESS"))
        .andExpect(jsonPath("$.items[*].key", contains(vpn.getKey())));
    mvc.perform(api.get(alice, "/api/v1/tickets?view=all&status=OPEN&q=mail"))
        .andExpect(jsonPath("$.items[*].key", contains(bobsTicket.getKey())));
    mvc.perform(api.get(alice, "/api/v1/tickets?view=mine&status=OPEN&q=mail"))
        .andExpect(jsonPath("$.items", hasSize(0)));
    mvc.perform(api.get(alice, "/api/v1/tickets?status=CLOSED"))
        .andExpect(jsonPath("$.items", hasSize(0)))
        .andExpect(jsonPath("$.totalItems").value(0));
  }

  @Test
  void paginates() throws Exception {
    mvc.perform(api.get(alice, "/api/v1/tickets?view=all&size=2&page=1"))
        .andExpect(jsonPath("$.items[*].key", contains(vpn.getKey())))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.totalItems").value(3))
        .andExpect(jsonPath("$.totalPages").value(2));
  }

  @Test
  void rejectsInvalidParameters() throws Exception {
    String longQ = "q".repeat(101);
    for (String query :
        new String[] {
          "q=" + longQ, "size=101", "size=0", "page=-1", "status=DONE", "view=everyone"
        }) {
      mvc.perform(api.get(alice, "/api/v1/tickets?" + query))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
          .andExpect(jsonPath("$.errors", hasSize(1)));
    }
  }
}
