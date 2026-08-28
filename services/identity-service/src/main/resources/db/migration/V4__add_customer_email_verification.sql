alter table identity_users
    add column email varchar(255);

alter table identity_users
    add column email_verification_token_hash varchar(128);

alter table identity_users
    add column email_verification_token_expires_at timestamp with time zone;

alter table identity_users
    add constraint uq_identity_users_email unique (email);

alter table identity_users
    add constraint uq_identity_users_email_verification_token unique (email_verification_token_hash);

create table identity_outbox_events (
    id uuid primary key,
    event_type varchar(100) not null,
    aggregate_id bigint not null,
    payload text not null,
    created_at timestamp with time zone not null,
    published_at timestamp with time zone,
    attempts integer not null,
    next_attempt_at timestamp with time zone not null,
    failed_at timestamp with time zone,
    last_error text
);

create index ix_identity_outbox_events_pending
    on identity_outbox_events (published_at, failed_at, next_attempt_at, created_at);
