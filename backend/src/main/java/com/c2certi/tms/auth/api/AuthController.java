package com.c2certi.tms.auth.api;

import com.c2certi.tms.auth.JwtService;
import com.c2certi.tms.auth.SessionCookieFactory;
import com.c2certi.tms.common.error.InvalidCredentialsException;
import com.c2certi.tms.common.web.CurrentUserProvider;
import com.c2certi.tms.user.domain.User;
import com.c2certi.tms.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final UserRepository users;
  private final JwtService jwtService;
  private final SessionCookieFactory cookies;
  private final CurrentUserProvider currentUser;

  public AuthController(
      AuthenticationManager authenticationManager,
      UserRepository users,
      JwtService jwtService,
      SessionCookieFactory cookies,
      CurrentUserProvider currentUser) {
    this.authenticationManager = authenticationManager;
    this.users = users;
    this.jwtService = jwtService;
    this.cookies = cookies;
    this.currentUser = currentUser;
  }

  @PostMapping("/login")
  public ResponseEntity<CurrentUserResponse> login(@Valid @RequestBody LoginRequest request) {
    try {
      authenticationManager.authenticate(
          UsernamePasswordAuthenticationToken.unauthenticated(
              request.username(), request.password()));
    } catch (AuthenticationException e) {
      throw new InvalidCredentialsException();
    }
    User user =
        users
            .findByUsernameIgnoreCase(request.username())
            .orElseThrow(InvalidCredentialsException::new);
    String token = jwtService.issue(user);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookies.create(token, jwtService.ttl()).toString())
        .body(CurrentUserResponse.from(user));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
        .build();
  }

  @GetMapping("/me")
  public CurrentUserResponse me() {
    return CurrentUserResponse.from(currentUser.require());
  }
}
