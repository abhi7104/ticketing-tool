package com.c2certi.tms.ticket.api.dto;

/** Validation constants shared by request DTOs that accept a priority. */
final class Priorities {

  static final String PATTERN = "LOW|MEDIUM|HIGH|CRITICAL";
  static final String MESSAGE = "Priority must be one of LOW, MEDIUM, HIGH, CRITICAL.";

  private Priorities() {}
}
