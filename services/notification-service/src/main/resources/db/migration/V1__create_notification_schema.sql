-- Draft initial schema for a fresh PostgreSQL notification-service database.
create table notifications (
    id uuid primary key,
    reservation_id uuid not null,
    recipient varchar(255) not null,
    subject varchar(255) not null,
    body text not null,
    status varchar(30) not null,
    attempts integer not null,
    last_attempt_at timestamp with time zone,
    last_error varchar(2000)
);

create unique index uq_notification_reservation_subject
    on notifications (reservation_id, subject);
