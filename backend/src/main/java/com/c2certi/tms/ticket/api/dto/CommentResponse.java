package com.c2certi.tms.ticket.api.dto;

import java.time.Instant;

public record CommentResponse(Long id, UserSummary author, String body, Instant createdAt) {}
