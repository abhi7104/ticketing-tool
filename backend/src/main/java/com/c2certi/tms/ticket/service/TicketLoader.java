package com.c2certi.tms.ticket.service;

import com.c2certi.tms.common.error.TicketNotFoundException;
import com.c2certi.tms.common.error.VersionConflictException;
import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.ticket.domain.TicketKeys;
import com.c2certi.tms.ticket.repository.TicketRepository;
import org.springframework.stereotype.Component;

/** Shared lookup used by every ticket use case: validates the key and applies version checks. */
@Component
public class TicketLoader {

  private final TicketRepository tickets;

  public TicketLoader(TicketRepository tickets) {
    this.tickets = tickets;
  }

  public Ticket load(String key) {
    TicketKeys.requireValid(key);
    return tickets.findByTicketKey(key).orElseThrow(() -> new TicketNotFoundException(key));
  }

  /** Loads the ticket and fails with 412 unless it still has {@code expectedVersion}. */
  public Ticket loadForUpdate(String key, long expectedVersion) {
    Ticket ticket = load(key);
    if (ticket.getVersion() != expectedVersion) {
      throw new VersionConflictException();
    }
    return ticket;
  }
}
