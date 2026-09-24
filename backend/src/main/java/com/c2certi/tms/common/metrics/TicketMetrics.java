package com.c2certi.tms.common.metrics;

import com.c2certi.tms.ticket.domain.TicketStatus;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Business metrics for tickets (exported via /actuator/prometheus). */
@Component
public class TicketMetrics {

  private final MeterRegistry registry;

  public TicketMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  public void ticketCreated() {
    registry.counter("tickets.created").increment();
  }

  public void transitionApplied(TicketStatus from, TicketStatus to) {
    registry.counter("tickets.transitions", "from", from.name(), "to", to.name()).increment();
  }

  public void transitionRejected(TicketStatus from, TicketStatus to) {
    registry
        .counter(
            "tickets.transition.rejected",
            "from",
            from.name(),
            "to",
            to == null ? "NONE" : to.name())
        .increment();
  }

  public void commentAdded() {
    registry.counter("tickets.comments.added").increment();
  }
}
