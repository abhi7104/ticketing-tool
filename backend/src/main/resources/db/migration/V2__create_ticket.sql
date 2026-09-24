CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE SEQUENCE ticket_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE ticket (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_key   VARCHAR(20)  NOT NULL,
    title        VARCHAR(150) NOT NULL,
    description  TEXT         NOT NULL,
    priority     VARCHAR(10)  NOT NULL,
    status       VARCHAR(15)  NOT NULL DEFAULT 'OPEN',
    reporter_id  BIGINT       NOT NULL REFERENCES app_user (id),
    assignee_id  BIGINT       NOT NULL REFERENCES app_user (id),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    resolved_at  TIMESTAMPTZ,
    closed_at    TIMESTAMPTZ,
    version      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_ticket_key UNIQUE (ticket_key),
    CONSTRAINT ck_ticket_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_ticket_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    CONSTRAINT ck_ticket_title_length CHECK (char_length(title) BETWEEN 3 AND 150),
    CONSTRAINT ck_ticket_description_length CHECK (char_length(description) BETWEEN 10 AND 5000)
);

CREATE INDEX ix_ticket_status ON ticket (status);
CREATE INDEX ix_ticket_reporter ON ticket (reporter_id);
CREATE INDEX ix_ticket_assignee ON ticket (assignee_id);
CREATE INDEX ix_ticket_updated ON ticket (updated_at DESC, id DESC);
CREATE INDEX ix_ticket_title_trgm ON ticket USING gin (lower(title) gin_trgm_ops);
CREATE INDEX ix_ticket_description_trgm ON ticket USING gin (lower(description) gin_trgm_ops);
