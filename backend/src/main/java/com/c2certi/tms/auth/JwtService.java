package com.c2certi.tms.auth;

import com.c2certi.tms.common.config.AppProperties;
import com.c2certi.tms.user.domain.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/** Issues and verifies HS256-signed session tokens. The key comes from {@code JWT_SECRET}. */
@Service
public class JwtService {

  static final String ISSUER = "tms";
  private static final int MIN_SECRET_BYTES = 32;

  private final JwtEncoder encoder;
  private final JwtDecoder decoder;
  private final Duration ttl;
  private final Clock clock;

  public JwtService(AppProperties props, Clock clock) {
    String secret = props.jwt() == null ? null : props.jwt().secret();
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "JWT_SECRET must be set to at least " + MIN_SECRET_BYTES + " bytes");
    }
    SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    this.ttl = props.jwt().ttl() == null ? Duration.ofHours(8) : props.jwt().ttl();
    this.clock = clock;
  }

  public String issue(User user) {
    Instant now = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject(String.valueOf(user.getId()))
            .claim("username", user.getUsername())
            .issuedAt(now)
            .expiresAt(now.plus(ttl))
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  public JwtDecoder decoder() {
    return decoder;
  }

  public Duration ttl() {
    return ttl;
  }
}
