package com.c2certi.tms.ticket.api;

import com.c2certi.tms.common.web.CurrentUserProvider;
import com.c2certi.tms.ticket.api.dto.TicketSummaryCountsResponse;
import com.c2certi.tms.ticket.service.TicketSummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketSummaryController {

  private final TicketSummaryService summaryService;
  private final CurrentUserProvider currentUser;

  public TicketSummaryController(
      TicketSummaryService summaryService, CurrentUserProvider currentUser) {
    this.summaryService = summaryService;
    this.currentUser = currentUser;
  }

  @GetMapping("/summary")
  public TicketSummaryCountsResponse summary() {
    return summaryService.summarize(currentUser.require());
  }
}
