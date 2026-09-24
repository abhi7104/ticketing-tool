package com.c2certi.tms.ticket.api;

import com.c2certi.tms.common.web.CurrentUserProvider;
import com.c2certi.tms.common.web.ETags;
import com.c2certi.tms.ticket.api.dto.TicketDetailResponse;
import com.c2certi.tms.ticket.api.dto.UpdateTicketRequest;
import com.c2certi.tms.ticket.service.TicketUpdateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketUpdateController {

  private final TicketUpdateService updateService;
  private final CurrentUserProvider currentUser;

  public TicketUpdateController(
      TicketUpdateService updateService, CurrentUserProvider currentUser) {
    this.updateService = updateService;
    this.currentUser = currentUser;
  }

  @PatchMapping("/{ticketKey}")
  public ResponseEntity<TicketDetailResponse> update(
      @PathVariable String ticketKey,
      @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
      @Valid @RequestBody UpdateTicketRequest request) {
    long expectedVersion = ETags.parseIfMatch(ifMatch);
    TicketDetailResponse updated =
        updateService.update(ticketKey, expectedVersion, request, currentUser.require());
    return ResponseEntity.ok().eTag(ETags.format(updated.version())).body(updated);
  }
}
