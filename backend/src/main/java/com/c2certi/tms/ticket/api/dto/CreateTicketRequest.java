package com.c2certi.tms.ticket.api.dto;

import com.c2certi.tms.ticket.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Enum-like fields are bound as strings and checked with Bean Validation, so an invalid priority is
 * reported together with every other field problem instead of aborting JSON parsing.
 */
public record CreateTicketRequest(
    @NotBlank(message = "Title is required.")
        @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters.")
        String title,
    @NotBlank(message = "Description is required.")
        @Size(
            min = 10,
            max = 5000,
            message = "Description must be between 10 and 5,000 characters.")
        String description,
    @NotNull(message = "Please choose a priority.")
        @Pattern(regexp = Priorities.PATTERN, message = Priorities.MESSAGE)
        String priority,
    @NotNull(message = "Please choose an assignee.") Long assigneeId) {

  public CreateTicketRequest(String title, String description, Priority priority, Long assigneeId) {
    this(title, description, priority == null ? null : priority.name(), assigneeId);
  }

  public Priority priorityValue() {
    return Priority.valueOf(priority);
  }
}
