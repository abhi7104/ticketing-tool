package com.c2certi.tms.ticket.service;

import com.c2certi.tms.common.metrics.TicketMetrics;
import com.c2certi.tms.history.service.HistoryRecorder;
import com.c2certi.tms.ticket.api.dto.CreateTicketRequest;
import com.c2certi.tms.ticket.api.dto.TicketDetailResponse;
import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.ticket.domain.TicketKeys;
import com.c2certi.tms.ticket.repository.TicketRepository;
import com.c2certi.tms.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketCreationService {

  private static final Logger log = LoggerFactory.getLogger(TicketCreationService.class);

  private final TicketRepository tickets;
  private final AssigneeResolver assignees;
  private final HistoryRecorder history;
  private final TicketDetailService details;
  private final TicketMetrics metrics;
  private final Clock clock;

  public TicketCreationService(
      TicketRepository tickets,
      AssigneeResolver assignees,
      HistoryRecorder history,
      TicketDetailService details,
      TicketMetrics metrics,
      Clock clock) {
    this.tickets = tickets;
    this.assignees = assignees;
    this.history = history;
    this.details = details;
    this.metrics = metrics;
    this.clock = clock;
  }

  @Transactional
  public TicketDetailResponse create(CreateTicketRequest request, User reporter) {
    User assignee = assignees.resolve(request.assigneeId());
    Instant now = clock.instant();
    Ticket ticket =
        Ticket.create(
            TicketKeys.of(tickets.nextTicketNumber()),
            request.title(),
            request.description(),
            request.priorityValue(),
            reporter,
            assignee,
            now);
    tickets.save(ticket);
    history.recordCreated(ticket, reporter, now);
    metrics.ticketCreated();
    log.info("Ticket {} created by user {}", ticket.getKey(), reporter.getId());
    return details.toDetail(ticket);
  }
}
