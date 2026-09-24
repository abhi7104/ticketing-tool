package com.c2certi.tms.ticket.api;

import com.c2certi.tms.comment.domain.Comment;
import com.c2certi.tms.history.domain.TicketHistoryEntry;
import com.c2certi.tms.ticket.api.dto.CommentResponse;
import com.c2certi.tms.ticket.api.dto.HistoryEntryResponse;
import com.c2certi.tms.ticket.api.dto.TicketDetailResponse;
import com.c2certi.tms.ticket.api.dto.TicketSummaryResponse;
import com.c2certi.tms.ticket.api.dto.UserSummary;
import com.c2certi.tms.ticket.domain.Ticket;
import java.util.List;

/** Maps entities to API DTOs; entities are never serialized directly. */
public final class TicketMapper {

  private TicketMapper() {}

  public static TicketSummaryResponse toSummary(Ticket t) {
    return new TicketSummaryResponse(
        t.getKey(),
        t.getTitle(),
        t.getStatus(),
        t.getPriority(),
        UserSummary.from(t.getAssignee()),
        UserSummary.from(t.getReporter()),
        t.getCreatedAt(),
        t.getUpdatedAt());
  }

  public static TicketDetailResponse toDetail(
      Ticket t, List<Comment> comments, List<TicketHistoryEntry> history) {
    return new TicketDetailResponse(
        t.getKey(),
        t.getTitle(),
        t.getDescription(),
        t.getStatus(),
        t.getPriority(),
        UserSummary.from(t.getAssignee()),
        UserSummary.from(t.getReporter()),
        t.getCreatedAt(),
        t.getUpdatedAt(),
        t.getResolvedAt(),
        t.getClosedAt(),
        t.getVersion(),
        t.getStatus().allowedNext().orElse(null),
        comments.stream().map(TicketMapper::toComment).toList(),
        history.stream().map(TicketMapper::toHistory).toList());
  }

  public static CommentResponse toComment(Comment c) {
    return new CommentResponse(
        c.getId(), UserSummary.from(c.getAuthor()), c.getBody(), c.getCreatedAt());
  }

  public static HistoryEntryResponse toHistory(TicketHistoryEntry h) {
    return new HistoryEntryResponse(
        h.getId(),
        UserSummary.from(h.getActor()),
        h.getChangeType(),
        h.getField(),
        h.getOldValue(),
        h.getNewValue(),
        h.getOccurredAt());
  }
}
