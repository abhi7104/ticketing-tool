package com.c2certi.tms.ticket.api;

import com.c2certi.tms.common.web.CurrentUserProvider;
import com.c2certi.tms.common.web.ETags;
import com.c2certi.tms.ticket.api.dto.CreateTicketRequest;
import com.c2certi.tms.ticket.api.dto.TicketDetailResponse;
import com.c2certi.tms.ticket.service.TicketCreationService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketCreationController {

  private final TicketCreationService creationService;
  private final CurrentUserProvider currentUser;

  public TicketCreationController(
      TicketCreationService creationService, CurrentUserProvider currentUser) {
    this.creationService = creationService;
    this.currentUser = currentUser;
  }

  @PostMapping
  public ResponseEntity<TicketDetailResponse> create(
      @Valid @RequestBody CreateTicketRequest request) {
    TicketDetailResponse created = creationService.create(request, currentUser.require());
    return ResponseEntity.created(URI.create("/api/v1/tickets/" + created.key()))
        .eTag(ETags.format(created.version()))
        .body(created);
  }
}
