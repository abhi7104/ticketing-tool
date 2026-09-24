package com.c2certi.tms.ticket.api.dto;

import com.c2certi.tms.ticket.domain.Priority;
import com.c2certi.tms.ticket.domain.TicketStatus;
import java.time.Instant;

public record TicketSummaryResponse(
    String key,
    String title,
    TicketStatus status,
    Priority priority,
    UserSummary assignee,
    UserSummary reporter,
    Instant createdAt,
    Instant updatedAt) {}
