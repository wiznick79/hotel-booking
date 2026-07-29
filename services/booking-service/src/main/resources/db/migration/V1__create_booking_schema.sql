-- Draft initial schema for a fresh PostgreSQL booking-service database.
create table reservations (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone,
    version integer not null,
    hotel_id varchar(255) not null,
    customer_username varchar(150),
    guest_name varchar(255) not null,
    guest_phone varchar(100) not null,
    guest_email varchar(255),
    guest_count integer not null,
    check_in_date date not null,
    check_out_date date not null,
    notes varchar(2000),
    total_price numeric(19, 2),
    currency varchar(10),
    discount_code varchar(100),
    discount_amount numeric(19, 2),
    payment_mode varchar(30) not null,
    manual_confirmation_required boolean not null,
    hold_until timestamp with time zone,
    guest_access_token_hash varchar(255),
    guest_access_token_expires_at timestamp with time zone,
    status varchar(30) not null
);

create table reservation_items (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone,
    version integer not null,
    reservation_id uuid not null references reservations(id),
    room_id varchar(255) not null
);

create table booking_policies (
    id uuid primary key,
    hotel_id varchar(255) not null unique,
    pay_later_allowed boolean not null,
    max_unconfirmed_bookings integer not null,
    hold_duration_minutes bigint not null,
    cancellation_deadline_days integer not null
);

create table discount_codes (
    id uuid primary key,
    hotel_id varchar(255) not null,
    code varchar(100) not null,
    percentage numeric(19, 2),
    fixed_amount numeric(19, 2),
    valid_from date not null,
    valid_until date not null,
    maximum_uses integer,
    used_count integer not null,
    active boolean not null,
    constraint uq_discount_code unique (hotel_id, code)
);

create table outbox_events (
    id uuid primary key,
    event_type varchar(100) not null,
    aggregate_id uuid,
    payload text not null,
    created_at timestamp with time zone not null,
    published_at timestamp with time zone,
    attempts integer not null,
    next_attempt_at timestamp with time zone not null,
    failed_at timestamp with time zone,
    last_error text
);

create table audit_logs (
    id uuid primary key,
    actor varchar(255),
    action varchar(100) not null,
    entity_type varchar(100) not null,
    entity_id uuid,
    details varchar(2000),
    created_at timestamp with time zone not null
);
