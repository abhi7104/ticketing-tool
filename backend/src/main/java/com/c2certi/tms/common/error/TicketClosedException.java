package com.c2certi.tms.common.error;

import org.springframework.http.HttpStatus;

public class TicketClosedException extends ApiException {

  public TicketClosedException() {
    super(
        ErrorCode.TICKET_CLOSED,
        HttpStatus.CONFLICT,
        "Ticket is closed",
        "Closed tickets can't be changed.");
  }
}
