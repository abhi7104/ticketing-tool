package com.c2certi.tms.ticket.domain;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Ticket lifecycle. The only allowed flow is OPEN → IN_PROGRESS → RESOLVED → CLOSED; CLOSED is
 * terminal. Every other transition, including staying in the same status, is invalid.
 */
public enum TicketStatus {
  OPEN("Open"),
  IN_PROGRESS("In progress"),
  RESOLVED("Resolved"),
  CLOSED("Closed");

  private final String label;

  TicketStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  /** The single status this one may move to, or empty when terminal. */
  public Optional<TicketStatus> allowedNext() {
    return switch (this) {
      case OPEN -> Optional.of(IN_PROGRESS);
      case IN_PROGRESS -> Optional.of(RESOLVED);
      case RESOLVED -> Optional.of(CLOSED);
      case CLOSED -> Optional.empty();
    };
  }

  /** Open and In progress tickets still need action from their assignee. */
  public boolean isPendingForAssignee() {
    return this == OPEN || this == IN_PROGRESS;
  }

  public static Set<TicketStatus> pendingForAssignee() {
    EnumSet<TicketStatus> pending = EnumSet.noneOf(TicketStatus.class);
    for (TicketStatus s : values()) {
      if (s.isPendingForAssignee()) {
        pending.add(s);
      }
    }
    return pending;
  }

  public boolean canTransitionTo(TicketStatus target) {
    return target != null && allowedNext().map(target::equals).orElse(false);
  }
}
