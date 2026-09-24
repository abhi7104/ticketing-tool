package com.c2certi.tms.history.repository;

import com.c2certi.tms.history.domain.TicketHistoryEntry;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketHistoryRepository extends JpaRepository<TicketHistoryEntry, Long> {

  @EntityGraph(attributePaths = "actor")
  List<TicketHistoryEntry> findByTicketIdOrderByOccurredAtAscIdAsc(Long ticketId);

  long countByTicketId(Long ticketId);
}
