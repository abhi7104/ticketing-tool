package com.c2certi.tms.ticket.api;

import com.c2certi.tms.common.web.ETags;
import com.c2certi.tms.ticket.api.dto.TicketDetailResponse;
import com.c2certi.tms.ticket.service.TicketDetailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketDetailController {

  private final TicketDetailService detailService;

  public TicketDetailController(TicketDetailService detailService) {
    this.detailService = detailService;
  }

  @GetMapping("/{ticketKey}")
  public ResponseEntity<TicketDetailResponse> get(@PathVariable String ticketKey) {
    TicketDetailResponse detail = detailService.getDetail(ticketKey);
    return ResponseEntity.ok().eTag(ETags.format(detail.version())).body(detail);
  }
}
