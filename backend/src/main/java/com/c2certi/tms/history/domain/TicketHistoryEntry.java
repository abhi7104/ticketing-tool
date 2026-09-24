package com.c2certi.tms.history.domain;

import com.c2certi.tms.ticket.domain.Ticket;
import com.c2certi.tms.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

/** Immutable audit record of one change to a ticket. */
@Entity
@Immutable
@Table(name = "ticket_history")
public class TicketHistoryEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
  private Ticket ticket;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "actor_id", nullable = false, updatable = false)
  private User actor;

  @Enumerated(EnumType.STRING)
  @Column(name = "change_type", nullable = false, updatable = false, length = 20)
  private ChangeType changeType;

  @Column(name = "field_name", updatable = false, length = 30)
  private String field;

  @Column(name = "old_value", updatable = false, columnDefinition = "text")
  private String oldValue;

  @Column(name = "new_value", updatable = false, columnDefinition = "text")
  private String newValue;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  protected TicketHistoryEntry() {}

  public TicketHistoryEntry(
      Ticket ticket,
      User actor,
      ChangeType changeType,
      String field,
      String oldValue,
      String newValue,
      Instant occurredAt) {
    this.ticket = ticket;
    this.actor = actor;
    this.changeType = changeType;
    this.field = field;
    this.oldValue = oldValue;
    this.newValue = newValue;
    this.occurredAt = occurredAt;
  }

  public Long getId() {
    return id;
  }

  public Ticket getTicket() {
    return ticket;
  }

  public User getActor() {
    return actor;
  }

  public ChangeType getChangeType() {
    return changeType;
  }

  public String getField() {
    return field;
  }

  public String getOldValue() {
    return oldValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
