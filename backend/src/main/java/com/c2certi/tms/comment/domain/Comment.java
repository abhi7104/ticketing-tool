package com.c2certi.tms.comment.domain;

import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Append-only comment on a ticket. */
@Entity
@Table(name = "ticket_comment")
public class Comment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
  private Ticket ticket;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id", nullable = false, updatable = false)
  private User author;

  @Column(nullable = false, updatable = false, columnDefinition = "text")
  private String body;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Comment() {}

  public Comment(Ticket ticket, User author, String body, Instant createdAt) {
    this.ticket = ticket;
    this.author = author;
    this.body = body;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Ticket getTicket() {
    return ticket;
  }

  public User getAuthor() {
    return author;
  }

  public String getBody() {
    return body;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
