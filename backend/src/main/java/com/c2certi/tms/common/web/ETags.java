package com.c2certi.tms.common.web;

import com.c2certi.tms.common.error.PreconditionRequiredException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts between ticket versions and HTTP entity tags ({@code "3"}). */
public final class ETags {

  private static final Pattern TAG = Pattern.compile("^(?:W/)?\"?(\\d{1,18})\"?$");

  private ETags() {}

  public static String format(long version) {
    return "\"" + version + "\"";
  }

  /** Parses the {@code If-Match} header; a missing or malformed value yields 428. */
  public static long parseIfMatch(String header) {
    if (header == null) {
      throw new PreconditionRequiredException();
    }
    Matcher m = TAG.matcher(header.trim());
    if (!m.matches()) {
      throw new PreconditionRequiredException();
    }
    return Long.parseLong(m.group(1));
  }
}
