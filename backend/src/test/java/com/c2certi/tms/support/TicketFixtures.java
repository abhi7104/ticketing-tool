package com.c2certi.tms.support;

import com.c2certi.tms.history.service.HistoryRecorder;
import com.c2certi.tms.ticket.domain.Priority;
import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.ticket.domain.TicketKeys;
import com.c2certi.tms.ticket.domain.TicketStatus;
import com.c2certi.tms.ticket.repository.TicketRepository;
import com.c2certi.tms.user.domain.User;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates tickets directly through the domain model (bypassing the API under test). */
@Component
public class TicketFixtures {

  private final TicketRepository tickets;
  private final HistoryRecorder history;

  public TicketFixtures(TicketRepository tickets, HistoryRecorder history) {
    this.tickets = tickets;
    this.history = history;
  }

  @Transactional
  public Ticket create(User reporter, User assignee, String title, String description) {
    Instant now = Instant.now();
    Ticket ticket =
        Ticket.create(
            TicketKeys.of(tickets.nextTicketNumber()),
            title,
            description,
            Priority.MEDIUM,
            reporter,
            assignee,
            now);
    tickets.save(ticket);
    history.recordCreated(ticket, reporter, now);
    return ticket;
  }

  @Transactional
  public Ticket create(User reporter) {
    return create(reporter, reporter, "Printer is broken", "The office printer shows error E42.");
  }

  /** Creates a ticket and walks it through valid transitions until it reaches {@code status}. */
  @Transactional
  public Ticket createInStatus(User reporter, TicketStatus status) {
    Ticket ticket = create(reporter);
    Ticket managed = tickets.findByTicketKey(ticket.getKey()).orElseThrow();
    while (managed.getStatus() != status) {
      TicketStatus next = managed.getStatus().allowedNext().orElseThrow();
      managed.transitionTo(next, Instant.now());
    }
    return tickets.saveAndFlush(managed);
  }
}
