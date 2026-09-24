package com.c2certi.tms.ticket.api.dto;

import com.c2certi.tms.ticket.domain.TicketStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Query parameters for {@code GET /api/v1/tickets}; null values take their defaults. */
public record TicketListQuery(
    @Size(max = 100, message = "Search text must be at most 100 characters.") String q,
    TicketStatus status,
    @Pattern(regexp = "mine|assigned|all", message = "View must be 'mine', 'assigned' or 'all'.")
        String view,
    @Min(value = 0, message = "Page must be 0 or greater.") Integer page,
    @Min(value = 1, message = "Page size must be between 1 and 100.")
        @Max(value = 100, message = "Page size must be between 1 and 100.")
        Integer size) {

  public static final int DEFAULT_SIZE = 20;

  public boolean mineOnly() {
    return view == null || view.equals("mine");
  }

  /** Tickets assigned to the current user that still need their action. */
  public boolean assignedOnly() {
    return "assigned".equals(view);
  }

  public int pageOrDefault() {
    return page == null ? 0 : page;
  }

  public int sizeOrDefault() {
    return size == null ? DEFAULT_SIZE : size;
  }

  public String keyword() {
    return q == null || q.isBlank() ? null : q.strip();
  }
}
