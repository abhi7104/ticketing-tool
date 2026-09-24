package com.c2certi.tms.ticket.service;

import com.c2certi.tms.common.error.InvalidStatusTransitionException;
import com.c2certi.tms.common.metrics.TicketMetrics;
import com.c2certi.tms.history.service.HistoryRecorder;
import com.c2certi.tms.ticket.api.dto.TicketDetailResponse;
import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.ticket.domain.TicketStatus;
import com.c2certi.tms.ticket.repository.TicketRepository;
import com.c2certi.tms.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies status changes; the ticket entity enforces the state machine. */
@Service
public class TicketTransitionService {

  private static final Logger log = LoggerFactory.getLogger(TicketTransitionService.class);

  private final TicketLoader loader;
  private final TicketRepository tickets;
  private final HistoryRecorder history;
  private final TicketDetailService details;
  private final TicketMetrics metrics;
  private final Clock clock;

  public TicketTransitionService(
      TicketLoader loader,
      TicketRepository tickets,
      HistoryRecorder history,
      TicketDetailService details,
      TicketMetrics metrics,
      Clock clock) {
    this.loader = loader;
    this.tickets = tickets;
    this.history = history;
    this.details = details;
    this.metrics = metrics;
    this.clock = clock;
  }

  @Transactional
  public TicketDetailResponse transition(
      String key, long expectedVersion, TicketStatus target, User actor) {
    Ticket ticket = loader.loadForUpdate(key, expectedVersion);
    TicketStatus from = ticket.getStatus();
    Instant now = clock.instant();
    try {
      ticket.transitionTo(target, now);
    } catch (InvalidStatusTransitionException e) {
      metrics.transitionRejected(from, target);
      log.info("Rejected status change {} -> {} on {}", from, target, key);
      throw e;
    }
    history.recordStatusChange(ticket, actor, from, target, now);
    // Flush now so a concurrent change surfaces as an optimistic-lock failure (412).
    tickets.saveAndFlush(ticket);
    metrics.transitionApplied(from, target);
    return details.toDetail(ticket);
  }
}
