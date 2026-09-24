package com.c2certi.tms.common.config;

import com.c2certi.tms.auth.CookieBearerTokenResolver;
import com.c2certi.tms.auth.JwtService;
import com.c2certi.tms.common.error.ErrorCode;
import com.c2certi.tms.common.security.ProblemResponseWriter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, ProblemResponseWriter problems, AppProperties props) throws Exception {
    CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    csrfRepository.setCookieCustomizer(c -> c.path("/").sameSite("Strict"));

    List<String> origins =
        props.cors() == null || props.cors().allowedOrigins() == null
            ? List.of()
            : props.cors().allowedOrigins().stream().filter(o -> !o.isBlank()).toList();
    if (origins.isEmpty()) {
      http.cors(cors -> cors.disable());
    } else {
      http.cors(cors -> cors.configurationSource(corsSource(origins)));
    }

    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(csrfRepository)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/logout")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info")
                    .permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            rs ->
                rs.bearerTokenResolver(new CookieBearerTokenResolver())
                    .jwt(jwt -> {})
                    .authenticationEntryPoint(
                        (req, res, ex) ->
                            problems.write(
                                res,
                                HttpStatus.UNAUTHORIZED,
                                ErrorCode.UNAUTHENTICATED,
                                "Session expired",
                                "Please sign in again.")))
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                        (req, res, e) ->
                            problems.write(
                                res,
                                HttpStatus.UNAUTHORIZED,
                                ErrorCode.UNAUTHENTICATED,
                                "Session expired",
                                "Please sign in again."))
                    .accessDeniedHandler(
                        (req, res, e) ->
                            problems.write(
                                res,
                                HttpStatus.FORBIDDEN,
                                ErrorCode.FORBIDDEN,
                                "Request blocked",
                                "Your security token is missing or expired. Please reload the"
                                    + " page and try again.")))
        .headers(
            h ->
                h.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'"))
                    .frameOptions(f -> f.deny()))
        .httpBasic(b -> b.disable())
        .formLogin(f -> f.disable())
        .logout(l -> l.disable());
    return http.build();
  }

  @Bean
  JwtDecoder jwtDecoder(JwtService jwtService) {
    return jwtService.decoder();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  AuthenticationManager authenticationManager(
      UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return new ProviderManager(provider);
  }

  private static CorsConfigurationSource corsSource(List<String> origins) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(origins);
    config.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
    config.setAllowedHeaders(List.of("Content-Type", "If-Match", "X-XSRF-TOKEN", "X-Request-Id"));
    config.setExposedHeaders(List.of("ETag", "Location", "X-Request-Id"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }
}
