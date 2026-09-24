package com.c2certi.tms.common.error;

/** One user-facing validation problem for a single input field. */
public record ApiFieldError(String field, String message) {}
