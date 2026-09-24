package com.c2certi.tms.ticket.api.dto;

import com.c2certi.tms.ticket.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record TransitionRequest(
    @NotNull(message = "Please choose the new status.") TicketStatus targetStatus) {}
