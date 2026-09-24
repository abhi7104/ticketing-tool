package com.c2certi.tms.comment.api;

import com.c2certi.tms.comment.service.CommentService;
import com.c2certi.tms.common.web.CurrentUserProvider;
import com.c2certi.tms.ticket.api.dto.CommentResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets/{ticketKey}/comments")
public class CommentController {

  private final CommentService commentService;
  private final CurrentUserProvider currentUser;

  public CommentController(CommentService commentService, CurrentUserProvider currentUser) {
    this.commentService = commentService;
    this.currentUser = currentUser;
  }

  @GetMapping
  public List<CommentResponse> list(@PathVariable String ticketKey) {
    return commentService.list(ticketKey);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CommentResponse add(
      @PathVariable String ticketKey, @Valid @RequestBody CreateCommentRequest request) {
    return commentService.add(ticketKey, request, currentUser.require());
  }
}
