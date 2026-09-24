package com.c2certi.tms.ticket.domain;

import com.c2certi.tms.common.error.FieldValidationException;
import java.util.regex.Pattern;

/** Human-readable ticket references such as {@code TMS-42}. */
public final class TicketKeys {

  public static final String PREFIX = "TMS-";
  private static final Pattern VALID = Pattern.compile("^TMS-[1-9][0-9]{0,15}$");

  private TicketKeys() {}

  public static String of(long number) {
    return PREFIX + number;
  }

  /** Rejects malformed references with a 400 before any lookup. */
  public static String requireValid(String key) {
    if (key == null || !VALID.matcher(key).matches()) {
      throw new FieldValidationException("ticketKey", "Ticket reference must look like TMS-123.");
    }
    return key;
  }
}
