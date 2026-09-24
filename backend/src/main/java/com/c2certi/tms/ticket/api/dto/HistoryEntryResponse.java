package com.c2certi.tms.ticket.api.dto;

import com.c2certi.tms.history.domain.ChangeType;
import java.time.Instant;

public record HistoryEntryResponse(
    Long id,
    UserSummary actor,
    ChangeType changeType,
    String field,
    String oldValue,
    String newValue,
    Instant occurredAt) {}
