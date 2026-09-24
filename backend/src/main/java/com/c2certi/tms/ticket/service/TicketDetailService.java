package com.c2certi.tms.ticket.service;

import com.c2certi.tms.comment.repository.CommentRepository;
import com.c2certi.tms.history.repository.TicketHistoryRepository;
import com.c2certi.tms.ticket.api.TicketMapper;
import com.c2certi.tms.ticket.api.dto.TicketDetailResponse;
import com.c2certi.tms.ticket.domain.Ticket;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketDetailService {

  private final TicketLoader loader;
  private final CommentRepository comments;
  private final TicketHistoryRepository history;

  public TicketDetailService(
      TicketLoader loader, CommentRepository comments, TicketHistoryRepository history) {
    this.loader = loader;
    this.comments = comments;
    this.history = history;
  }

  @Transactional(readOnly = true)
  public TicketDetailResponse getDetail(String key) {
    return toDetail(loader.load(key));
  }

  /** Builds the detail view for an already-loaded ticket (inside the caller's transaction). */
  @Transactional(readOnly = true)
  public TicketDetailResponse toDetail(Ticket ticket) {
    return TicketMapper.toDetail(
        ticket,
        comments.findByTicketIdOrderByCreatedAtAscIdAsc(ticket.getId()),
        history.findByTicketIdOrderByOccurredAtAscIdAsc(ticket.getId()));
  }
}
