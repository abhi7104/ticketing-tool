package com.c2certi.tms.ticket.service;

import com.c2certi.tms.ticket.api.TicketMapper;
import com.c2certi.tms.ticket.api.dto.TicketListQuery;
import com.c2certi.tms.ticket.api.dto.TicketPageResponse;
import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.ticket.repository.TicketRepository;
import com.c2certi.tms.ticket.repository.TicketSpecifications;
import com.c2certi.tms.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketListService {

  private static final Sort NEWEST_UPDATED_FIRST =
      Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));

  /** "Assigned to me": most urgent first, then longest waiting, then by reference. */
  private static final Sort ASSIGNED_QUEUE_ORDER =
      Sort.by(Sort.Order.desc("priorityRank"), Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

  private final TicketRepository tickets;

  public TicketListService(TicketRepository tickets) {
    this.tickets = tickets;
  }

  @Transactional(readOnly = true)
  public TicketPageResponse list(TicketListQuery query, User currentUser) {
    Specification<Ticket> spec = Specification.where(null);
    if (query.assignedOnly()) {
      spec =
          spec.and(TicketSpecifications.assignedTo(currentUser.getId()))
              .and(TicketSpecifications.pendingForAssignee());
    } else if (query.mineOnly()) {
      spec = spec.and(TicketSpecifications.reportedBy(currentUser.getId()));
    }
    if (query.status() != null) {
      spec = spec.and(TicketSpecifications.hasStatus(query.status()));
    }
    if (query.keyword() != null) {
      spec = spec.and(TicketSpecifications.matchesKeyword(query.keyword()));
    }
    Page<Ticket> page =
        tickets.findAll(
            spec,
            PageRequest.of(
                query.pageOrDefault(),
                query.sizeOrDefault(),
                query.assignedOnly() ? ASSIGNED_QUEUE_ORDER : NEWEST_UPDATED_FIRST));
    return new TicketPageResponse(
        page.getContent().stream().map(TicketMapper::toSummary).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
