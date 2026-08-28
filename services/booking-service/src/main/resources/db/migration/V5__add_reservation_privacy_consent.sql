alter table reservations
    add column privacy_notice_accepted boolean not null default false,
    add column privacy_notice_accepted_at timestamp with time zone;
