package com.c2certi.tms.ticket.api;

import com.c2certi.tms.common.web.CurrentUserProvider;
import com.c2certi.tms.common.web.ETags;
import com.c2certi.tms.ticket.api.dto.TicketDetailResponse;
import com.c2certi.tms.ticket.api.dto.TransitionRequest;
import com.c2certi.tms.ticket.service.TicketTransitionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketTransitionController {

  private final TicketTransitionService transitionService;
  private final CurrentUserProvider currentUser;

  public TicketTransitionController(
      TicketTransitionService transitionService, CurrentUserProvider currentUser) {
    this.transitionService = transitionService;
    this.currentUser = currentUser;
  }

  @PostMapping("/{ticketKey}/transitions")
  public ResponseEntity<TicketDetailResponse> transition(
      @PathVariable String ticketKey,
      @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
      @Valid @RequestBody TransitionRequest request) {
    long expectedVersion = ETags.parseIfMatch(ifMatch);
    TicketDetailResponse updated =
        transitionService.transition(
            ticketKey, expectedVersion, request.targetStatus(), currentUser.require());
    return ResponseEntity.ok().eTag(ETags.format(updated.version())).body(updated);
  }
}
