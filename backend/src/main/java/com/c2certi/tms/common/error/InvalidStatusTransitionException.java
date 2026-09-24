package com.c2certi.tms.common.error;

import com.c2certi.tms.ticket.domain.TicketStatus;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class InvalidStatusTransitionException extends ApiException {

  private final TicketStatus current;
  private final TicketStatus target;

  public InvalidStatusTransitionException(TicketStatus current, TicketStatus target) {
    super(
        ErrorCode.INVALID_STATUS_TRANSITION,
        HttpStatus.CONFLICT,
        "Status change not allowed",
        describe(current, target));
    this.current = current;
    this.target = target;
  }

  private static String describe(TicketStatus current, TicketStatus target) {
    String from = current.label();
    String requested = target == null ? "the requested status" : target.label();
    return current
        .allowedNext()
        .map(
            next ->
                "A ticket that is "
                    + from
                    + " can't move to "
                    + requested
                    + ". It can only move to "
                    + next.label()
                    + ".")
        .orElse("This ticket is " + from + " and its status can no longer change.");
  }

  public TicketStatus current() {
    return current;
  }

  public TicketStatus target() {
    return target;
  }

  @Override
  public Map<String, Object> properties() {
    Map<String, Object> props = new HashMap<>();
    props.put("currentStatus", current.name());
    props.put("allowedNextStatus", current.allowedNext().map(Enum::name).orElse(null));
    return props;
  }
}
