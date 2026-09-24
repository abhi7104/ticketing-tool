package com.c2certi.tms.common.error;

import org.springframework.http.HttpStatus;

public class TicketNotFoundException extends ApiException {

  public TicketNotFoundException(String ticketKey) {
    super(
        ErrorCode.TICKET_NOT_FOUND,
        HttpStatus.NOT_FOUND,
        "Ticket not found",
        "We couldn't find ticket " + ticketKey + ".");
  }
}
