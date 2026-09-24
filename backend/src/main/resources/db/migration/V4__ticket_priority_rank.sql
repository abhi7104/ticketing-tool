-- Numeric priority for sorting (text sorts alphabetically). Computed by PostgreSQL, never written
-- by the application.
ALTER TABLE ticket
    ADD COLUMN priority_rank SMALLINT GENERATED ALWAYS AS (
        CASE priority
            WHEN 'CRITICAL' THEN 4
            WHEN 'HIGH' THEN 3
            WHEN 'MEDIUM' THEN 2
            ELSE 1
        END
    ) STORED;

-- Serves the "Assigned to me" view and its count.
CREATE INDEX ix_ticket_assigned_queue
    ON ticket (assignee_id, priority_rank DESC, created_at, id)
    WHERE status IN ('OPEN', 'IN_PROGRESS');
