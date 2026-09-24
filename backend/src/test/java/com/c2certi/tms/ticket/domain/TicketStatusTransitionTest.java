package com.c2certi.tms.ticket.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.c2certi.tms.common.error.InvalidStatusTransitionException;
import com.c2certi.tms.user.domain.User;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TicketStatusTransitionTest {

  private static final Set<String> ALLOWED =
      Set.of("OPEN->IN_PROGRESS", "IN_PROGRESS->RESOLVED", "RESOLVED->CLOSED");

  static Stream<Arguments> allPairs() {
    return EnumSet.allOf(TicketStatus.class).stream()
        .flatMap(
            from -> EnumSet.allOf(TicketStatus.class).stream().map(to -> Arguments.of(from, to)));
  }

  @Test
  void matrixHasSixteenPairsWithThreeAllowed() {
    assertThat(allPairs().count()).isEqualTo(16);
    long allowed =
        allPairs()
            .filter(a -> ((TicketStatus) a.get()[0]).canTransitionTo((TicketStatus) a.get()[1]))
            .count();
    assertThat(allowed).isEqualTo(3);
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @MethodSource("allPairs")
  void canTransitionToMatchesStateMachine(TicketStatus from, TicketStatus to) {
    assertThat(from.canTransitionTo(to)).isEqualTo(ALLOWED.contains(from + "->" + to));
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @MethodSource("allPairs")
  void ticketTransitionToEnforcesStateMachine(TicketStatus from, TicketStatus to) {
    Ticket ticket = ticketIn(from);
    Instant later = Instant.parse("2026-09-21T12:00:00Z");

    if (ALLOWED.contains(from + "->" + to)) {
      ticket.transitionTo(to, later);
      assertThat(ticket.getStatus()).isEqualTo(to);
      assertThat(ticket.getUpdatedAt()).isEqualTo(later);
      if (to == TicketStatus.RESOLVED) {
        assertThat(ticket.getResolvedAt()).isEqualTo(later);
      }
      if (to == TicketStatus.CLOSED) {
        assertThat(ticket.getClosedAt()).isEqualTo(later);
      }
    } else {
      Instant before = ticket.getUpdatedAt();
      assertThatThrownBy(() -> ticket.transitionTo(to, later))
          .isInstanceOf(InvalidStatusTransitionException.class)
          .satisfies(
              e -> {
                InvalidStatusTransitionException ex = (InvalidStatusTransitionException) e;
                assertThat(ex.current()).isEqualTo(from);
                assertThat(ex.properties().get("currentStatus")).isEqualTo(from.name());
              });
      assertThat(ticket.getStatus()).isEqualTo(from);
      assertThat(ticket.getUpdatedAt()).isEqualTo(before);
    }
  }

  @Test
  void nullTargetIsRejected() {
    Ticket ticket = ticketIn(TicketStatus.OPEN);
    assertThatThrownBy(() -> ticket.transitionTo(null, Instant.now()))
        .isInstanceOf(InvalidStatusTransitionException.class);
  }

  @Test
  void allowedNextValues() {
    assertThat(TicketStatus.OPEN.allowedNext()).contains(TicketStatus.IN_PROGRESS);
    assertThat(TicketStatus.IN_PROGRESS.allowedNext()).contains(TicketStatus.RESOLVED);
    assertThat(TicketStatus.RESOLVED.allowedNext()).contains(TicketStatus.CLOSED);
    assertThat(TicketStatus.CLOSED.allowedNext()).isEmpty();
  }

  static Ticket ticketIn(TicketStatus status) {
    User user = new User("alice", "Alice", "alice@example.test", "x", Instant.EPOCH);
    Ticket ticket =
        Ticket.create(
            "TMS-1",
            "Printer broken",
            "Printer shows error E42",
            Priority.LOW,
            user,
            user,
            Instant.parse("2026-01-01T00:00:00Z"));
    Instant t = Instant.parse("2026-01-01T00:00:00Z");
    while (ticket.getStatus() != status) {
      ticket.transitionTo(ticket.getStatus().allowedNext().orElseThrow(), t);
    }
    return ticket;
  }
}
