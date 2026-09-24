CREATE TABLE ticket_comment (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id  BIGINT      NOT NULL REFERENCES ticket (id),
    author_id  BIGINT      NOT NULL REFERENCES app_user (id),
    body       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_ticket_comment_body_length CHECK (char_length(body) BETWEEN 1 AND 2000)
);

CREATE INDEX ix_ticket_comment_ticket ON ticket_comment (ticket_id, created_at);

CREATE TABLE ticket_history (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id   BIGINT      NOT NULL REFERENCES ticket (id),
    actor_id    BIGINT      NOT NULL REFERENCES app_user (id),
    change_type VARCHAR(20) NOT NULL,
    field_name  VARCHAR(30),
    old_value   TEXT,
    new_value   TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_ticket_history_change_type
        CHECK (change_type IN ('CREATED', 'FIELD_UPDATED', 'STATUS_CHANGED'))
);

CREATE INDEX ix_ticket_history_ticket ON ticket_history (ticket_id, occurred_at);
