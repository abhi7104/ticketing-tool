package com.c2certi.tms.ticket.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TicketPendingStatusTest {

  @ParameterizedTest
  @EnumSource(TicketStatus.class)
  void onlyOpenAndInProgressArePendingForTheAssignee(TicketStatus status) {
    boolean expected = status == TicketStatus.OPEN || status == TicketStatus.IN_PROGRESS;
    assertThat(status.isPendingForAssignee()).isEqualTo(expected);
  }

  @Test
  void pendingSetIsExactlyOpenAndInProgress() {
    assertThat(TicketStatus.pendingForAssignee())
        .containsExactlyInAnyOrder(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
  }
}
