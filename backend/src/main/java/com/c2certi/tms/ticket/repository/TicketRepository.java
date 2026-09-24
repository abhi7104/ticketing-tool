package com.c2certi.tms.ticket.repository;

import com.c2certi.tms.ticket.domain.Ticket;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TicketRepository
    extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

  @EntityGraph(attributePaths = {"reporter", "assignee"})
  Optional<Ticket> findByTicketKey(String ticketKey);

  @Override
  @EntityGraph(attributePaths = {"reporter", "assignee"})
  Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

  @Query(value = "SELECT nextval('ticket_number_seq')", nativeQuery = true)
  long nextTicketNumber();
}
