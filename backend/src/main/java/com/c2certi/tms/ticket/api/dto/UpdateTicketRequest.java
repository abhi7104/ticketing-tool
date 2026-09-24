package com.c2certi.tms.ticket.api.dto;

import com.c2certi.tms.ticket.domain.Priority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Partial update: only non-null fields change. Status is deliberately absent. */
public record UpdateTicketRequest(
    @Pattern(regexp = "(?s).*\\S.*", message = "Title is required.")
        @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters.")
        String title,
    @Pattern(regexp = "(?s).*\\S.*", message = "Description is required.")
        @Size(
            min = 10,
            max = 5000,
            message = "Description must be between 10 and 5,000 characters.")
        String description,
    @Pattern(regexp = Priorities.PATTERN, message = Priorities.MESSAGE) String priority,
    Long assigneeId) {

  public UpdateTicketRequest(String title, String description, Priority priority, Long assigneeId) {
    this(title, description, priority == null ? null : priority.name(), assigneeId);
  }

  public Priority priorityValue() {
    return priority == null ? null : Priority.valueOf(priority);
  }

  @AssertTrue(message = "Please change at least one field.")
  public boolean isAnyFieldPresent() {
    return title != null || description != null || priority != null || assigneeId != null;
  }
}
