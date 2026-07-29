alter table outbox_events
    add column next_attempt_at timestamp with time zone not null default current_timestamp,
    add column failed_at timestamp with time zone,
    add column last_error text;
