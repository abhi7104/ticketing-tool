package com.c2certi.tms.ticket.api.dto;

import java.util.List;

public record TicketPageResponse(
    List<TicketSummaryResponse> items, int page, int size, long totalItems, int totalPages) {}
