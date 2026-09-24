package com.c2certi.tms.comment.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
    @NotBlank(message = "Comment can't be empty.")
        @Size(max = 2000, message = "Comment must be at most 2,000 characters.")
        String body) {}
