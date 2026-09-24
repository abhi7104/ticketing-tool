package com.c2certi.tms.ticket.api;

import com.c2certi.tms.common.web.CurrentUserProvider;
import com.c2certi.tms.ticket.api.dto.TicketListQuery;
import com.c2certi.tms.ticket.api.dto.TicketPageResponse;
import com.c2certi.tms.ticket.service.TicketListService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketListController {

  private final TicketListService listService;
  private final CurrentUserProvider currentUser;

  public TicketListController(TicketListService listService, CurrentUserProvider currentUser) {
    this.listService = listService;
    this.currentUser = currentUser;
  }

  @GetMapping
  public TicketPageResponse list(@Valid @ModelAttribute TicketListQuery query) {
    return listService.list(query, currentUser.require());
  }
}
