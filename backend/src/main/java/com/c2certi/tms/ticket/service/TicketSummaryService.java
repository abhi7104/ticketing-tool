package com.c2certi.tms.ticket.service;

import com.c2certi.tms.ticket.api.dto.TicketSummaryCountsResponse;
import com.c2certi.tms.ticket.repository.TicketRepository;
import com.c2certi.tms.ticket.repository.TicketSpecifications;
import com.c2certi.tms.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketSummaryService {

  private final TicketRepository tickets;

  public TicketSummaryService(TicketRepository tickets) {
    this.tickets = tickets;
  }

  /** Uses the same rules as the "Assigned to me" view, so the numbers always agree. */
  @Transactional(readOnly = true)
  public TicketSummaryCountsResponse summarize(User currentUser) {
    long assignedPending =
        tickets.count(
            TicketSpecifications.assignedTo(currentUser.getId())
                .and(TicketSpecifications.pendingForAssignee()));
    return new TicketSummaryCountsResponse(assignedPending);
  }
}
