package com.c2certi.tms.support;

import java.security.SecureRandom;
import java.util.Base64;

/** Random per-run secrets so no credential is ever committed with the tests. */
public final class TestSecrets {

  public static final String JWT_SECRET = random(48);
  public static final String USER_PASSWORD = random(18);

  private TestSecrets() {}

  private static String random(int bytes) {
    byte[] buf = new byte[bytes];
    new SecureRandom().nextBytes(buf);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
  }
}
