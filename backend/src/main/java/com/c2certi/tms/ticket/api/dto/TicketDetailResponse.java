package com.c2certi.tms.ticket.api.dto;

import com.c2certi.tms.ticket.domain.Priority;
import com.c2certi.tms.ticket.domain.TicketStatus;
import java.time.Instant;
import java.util.List;

public record TicketDetailResponse(
    String key,
    String title,
    String description,
    TicketStatus status,
    Priority priority,
    UserSummary assignee,
    UserSummary reporter,
    Instant createdAt,
    Instant updatedAt,
    Instant resolvedAt,
    Instant closedAt,
    long version,
    TicketStatus allowedNextStatus,
    List<CommentResponse> comments,
    List<HistoryEntryResponse> history) {}
