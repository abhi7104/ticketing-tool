package com.c2certi.tms.history.service;

import com.c2certi.tms.history.domain.ChangeType;
import com.c2certi.tms.history.domain.TicketHistoryEntry;
import com.c2certi.tms.history.repository.TicketHistoryRepository;
import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.ticket.domain.TicketStatus;
import com.c2certi.tms.user.domain.User;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes audit entries in the same transaction as the change they describe. */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class HistoryRecorder {

  private final TicketHistoryRepository history;

  public HistoryRecorder(TicketHistoryRepository history) {
    this.history = history;
  }

  public void recordCreated(Ticket ticket, User actor, Instant at) {
    history.save(new TicketHistoryEntry(ticket, actor, ChangeType.CREATED, null, null, null, at));
  }

  public void recordFieldChange(
      Ticket ticket, User actor, String field, String oldValue, String newValue, Instant at) {
    history.save(
        new TicketHistoryEntry(
            ticket, actor, ChangeType.FIELD_UPDATED, field, oldValue, newValue, at));
  }

  public void recordStatusChange(
      Ticket ticket, User actor, TicketStatus from, TicketStatus to, Instant at) {
    history.save(
        new TicketHistoryEntry(
            ticket, actor, ChangeType.STATUS_CHANGED, "status", from.name(), to.name(), at));
  }
}
