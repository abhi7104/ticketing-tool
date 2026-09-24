package com.c2certi.tms.comment.service;

import com.c2certi.tms.comment.api.CreateCommentRequest;
import com.c2certi.tms.comment.domain.Comment;
import com.c2certi.tms.comment.repository.CommentRepository;
import com.c2certi.tms.common.metrics.TicketMetrics;
import com.c2certi.tms.ticket.api.TicketMapper;
import com.c2certi.tms.ticket.api.dto.CommentResponse;
import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.ticket.service.TicketLoader;
import com.c2certi.tms.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

  private final TicketLoader loader;
  private final CommentRepository comments;
  private final TicketMetrics metrics;
  private final Clock clock;

  public CommentService(
      TicketLoader loader, CommentRepository comments, TicketMetrics metrics, Clock clock) {
    this.loader = loader;
    this.comments = comments;
    this.metrics = metrics;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<CommentResponse> list(String key) {
    Ticket ticket = loader.load(key);
    return comments.findByTicketIdOrderByCreatedAtAscIdAsc(ticket.getId()).stream()
        .map(TicketMapper::toComment)
        .toList();
  }

  @Transactional
  public CommentResponse add(String key, CreateCommentRequest request, User author) {
    Ticket ticket = loader.load(key);
    ticket.assertOpenForChanges();
    Instant now = clock.instant();
    Comment comment = comments.save(new Comment(ticket, author, request.body(), now));
    ticket.touch(now);
    metrics.commentAdded();
    return TicketMapper.toComment(comment);
  }
}
