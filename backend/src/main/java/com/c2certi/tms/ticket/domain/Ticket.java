package com.c2certi.tms.ticket.domain;

import com.c2certi.tms.common.error.InvalidStatusTransitionException;
import com.c2certi.tms.common.error.TicketClosedException;
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
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * Support ticket aggregate. Status only changes through {@link #transitionTo}, which enforces the
 * {@link TicketStatus} state machine; closed tickets reject every other change.
 */
@Entity
@Table(name = "ticket")
public class Ticket {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "ticket_key", nullable = false, updatable = false, length = 20)
  private String ticketKey;

  @Column(nullable = false, length = 150)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Priority priority;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 15)
  private TicketStatus status;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reporter_id", nullable = false, updatable = false)
  private User reporter;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "assignee_id", nullable = false)
  private User assignee;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  /** Numeric priority computed by the database (V4 migration); used for sorting only. */
  @Generated(event = {EventType.INSERT, EventType.UPDATE})
  @Column(name = "priority_rank", insertable = false, updatable = false)
  private Short priorityRank;

  @Version
  @Column(nullable = false)
  private long version;

  protected Ticket() {}

  public static Ticket create(
      String key,
      String title,
      String description,
      Priority priority,
      User reporter,
      User assignee,
      Instant now) {
    Ticket t = new Ticket();
    t.ticketKey = key;
    t.title = title;
    t.description = description;
    t.priority = priority;
    t.status = TicketStatus.OPEN;
    t.reporter = reporter;
    t.assignee = assignee;
    t.createdAt = now;
    t.updatedAt = now;
    return t;
  }

  /** Moves to {@code target}, or throws without changing anything if the move is not allowed. */
  public void transitionTo(TicketStatus target, Instant now) {
    if (!status.canTransitionTo(target)) {
      throw new InvalidStatusTransitionException(status, target);
    }
    status = target;
    if (target == TicketStatus.RESOLVED) {
      resolvedAt = now;
    } else if (target == TicketStatus.CLOSED) {
      closedAt = now;
    }
    updatedAt = now;
  }

  public void assertOpenForChanges() {
    if (status == TicketStatus.CLOSED) {
      throw new TicketClosedException();
    }
  }

  public void updateTitle(String newTitle, Instant now) {
    assertOpenForChanges();
    title = newTitle;
    updatedAt = now;
  }

  public void updateDescription(String newDescription, Instant now) {
    assertOpenForChanges();
    description = newDescription;
    updatedAt = now;
  }

  public void updatePriority(Priority newPriority, Instant now) {
    assertOpenForChanges();
    priority = newPriority;
    updatedAt = now;
  }

  public void updateAssignee(User newAssignee, Instant now) {
    assertOpenForChanges();
    assignee = newAssignee;
    updatedAt = now;
  }

  /** Records activity (e.g. a new comment) without changing ticket fields. */
  public void touch(Instant now) {
    updatedAt = now;
  }

  public Long getId() {
    return id;
  }

  public String getKey() {
    return ticketKey;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public Priority getPriority() {
    return priority;
  }

  public TicketStatus getStatus() {
    return status;
  }

  public User getReporter() {
    return reporter;
  }

  public User getAssignee() {
    return assignee;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public Short getPriorityRank() {
    return priorityRank;
  }

  public long getVersion() {
    return version;
  }
}
