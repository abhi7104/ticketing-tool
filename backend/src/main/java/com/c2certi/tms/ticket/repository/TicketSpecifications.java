package com.c2certi.tms.ticket.repository;

import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.ticket.domain.TicketStatus;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Composable filters for the ticket listing. */
public final class TicketSpecifications {

  private static final char ESCAPE = '\\';

  private TicketSpecifications() {}

  public static Specification<Ticket> reportedBy(Long userId) {
    return (root, query, cb) -> cb.equal(root.get("reporter").get("id"), userId);
  }

  public static Specification<Ticket> assignedTo(Long userId) {
    return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), userId);
  }

  /** Tickets whose status still needs the assignee's action (Open, In progress). */
  public static Specification<Ticket> pendingForAssignee() {
    return (root, query, cb) -> root.get("status").in(TicketStatus.pendingForAssignee());
  }

  public static Specification<Ticket> hasStatus(TicketStatus status) {
    return (root, query, cb) -> cb.equal(root.get("status"), status);
  }

  /** Case-insensitive substring match on key, title or description; wildcards are literal. */
  public static Specification<Ticket> matchesKeyword(String keyword) {
    String pattern = "%" + escape(keyword.toLowerCase(Locale.ROOT)) + "%";
    return (root, query, cb) ->
        cb.or(
            cb.like(cb.lower(root.get("ticketKey")), pattern, ESCAPE),
            cb.like(cb.lower(root.get("title")), pattern, ESCAPE),
            cb.like(cb.lower(root.get("description")), pattern, ESCAPE));
  }

  static String escape(String value) {
    StringBuilder sb = new StringBuilder(value.length());
    for (char c : value.toCharArray()) {
      if (c == '%' || c == '_' || c == ESCAPE) {
        sb.append(ESCAPE);
      }
      sb.append(c);
    }
    return sb.toString();
  }
}
