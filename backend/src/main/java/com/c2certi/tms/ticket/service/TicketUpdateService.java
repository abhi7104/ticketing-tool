package com.c2certi.tms.ticket.service;

import com.c2certi.tms.history.service.HistoryRecorder;
import com.c2certi.tms.ticket.api.dto.TicketDetailResponse;
import com.c2certi.tms.ticket.api.dto.UpdateTicketRequest;
import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.ticket.repository.TicketRepository;
import com.c2certi.tms.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Updates editable fields and writes one history entry per field that actually changed. */
@Service
public class TicketUpdateService {

  private final TicketLoader loader;
  private final TicketRepository tickets;
  private final AssigneeResolver assignees;
  private final HistoryRecorder history;
  private final TicketDetailService details;
  private final Clock clock;

  public TicketUpdateService(
      TicketLoader loader,
      TicketRepository tickets,
      AssigneeResolver assignees,
      HistoryRecorder history,
      TicketDetailService details,
      Clock clock) {
    this.loader = loader;
    this.tickets = tickets;
    this.assignees = assignees;
    this.history = history;
    this.details = details;
    this.clock = clock;
  }

  @Transactional
  public TicketDetailResponse update(
      String key, long expectedVersion, UpdateTicketRequest request, User actor) {
    Ticket ticket = loader.loadForUpdate(key, expectedVersion);
    ticket.assertOpenForChanges();
    User newAssignee =
        request.assigneeId() == null ? null : assignees.resolve(request.assigneeId());
    Instant now = clock.instant();

    if (request.title() != null && !request.title().equals(ticket.getTitle())) {
      history.recordFieldChange(ticket, actor, "title", ticket.getTitle(), request.title(), now);
      ticket.updateTitle(request.title(), now);
    }
    if (request.description() != null && !request.description().equals(ticket.getDescription())) {
      history.recordFieldChange(
          ticket, actor, "description", ticket.getDescription(), request.description(), now);
      ticket.updateDescription(request.description(), now);
    }
    if (request.priorityValue() != null && request.priorityValue() != ticket.getPriority()) {
      history.recordFieldChange(
          ticket, actor, "priority", ticket.getPriority().name(), request.priority(), now);
      ticket.updatePriority(request.priorityValue(), now);
    }
    if (newAssignee != null && !Objects.equals(newAssignee.getId(), ticket.getAssignee().getId())) {
      history.recordFieldChange(
          ticket,
          actor,
          "assignee",
          ticket.getAssignee().getDisplayName(),
          newAssignee.getDisplayName(),
          now);
      ticket.updateAssignee(newAssignee, now);
    }
    tickets.saveAndFlush(ticket);
    return details.toDetail(ticket);
  }
}
