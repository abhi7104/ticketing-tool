package com.c2certi.tms.ticket.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.c2certi.tms.common.error.TicketClosedException;
import com.c2certi.tms.user.domain.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TicketTest {

  private final User other = new User("bob", "Bob", "bob@example.test", "x", Instant.EPOCH);
  private final Instant later = Instant.parse("2026-09-21T12:00:00Z");

  @Test
  void newTicketStartsOpen() {
    Ticket ticket = TicketStatusTransitionTest.ticketIn(TicketStatus.OPEN);
    assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
    assertThat(ticket.getCreatedAt()).isEqualTo(ticket.getUpdatedAt());
  }

  @Test
  void updatesBumpUpdatedAt() {
    Ticket ticket = TicketStatusTransitionTest.ticketIn(TicketStatus.IN_PROGRESS);
    ticket.updateTitle("New title", later);
    ticket.updateDescription("A longer description", later);
    ticket.updatePriority(Priority.CRITICAL, later);
    ticket.updateAssignee(other, later);
    assertThat(ticket.getTitle()).isEqualTo("New title");
    assertThat(ticket.getPriority()).isEqualTo(Priority.CRITICAL);
    assertThat(ticket.getAssignee()).isSameAs(other);
    assertThat(ticket.getUpdatedAt()).isEqualTo(later);
  }

  @Test
  void closedTicketRejectsEveryFieldChange() {
    Ticket ticket = TicketStatusTransitionTest.ticketIn(TicketStatus.CLOSED);
    assertThatThrownBy(() -> ticket.updateTitle("x", later))
        .isInstanceOf(TicketClosedException.class);
    assertThatThrownBy(() -> ticket.updateDescription("xxxxxxxxxxxx", later))
        .isInstanceOf(TicketClosedException.class);
    assertThatThrownBy(() -> ticket.updatePriority(Priority.LOW, later))
        .isInstanceOf(TicketClosedException.class);
    assertThatThrownBy(() -> ticket.updateAssignee(other, later))
        .isInstanceOf(TicketClosedException.class);
    assertThatThrownBy(ticket::assertOpenForChanges).isInstanceOf(TicketClosedException.class);
    assertThat(ticket.getTitle()).isEqualTo("Printer broken");
  }
}
