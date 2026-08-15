create table payment_attempts (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone,
    version integer not null,
    reservation_id uuid not null references reservations(id),
    provider varchar(50) not null,
    payment_method varchar(30) not null,
    status varchar(30) not null,
    provider_payment_id varchar(255) not null,
    redirect_url varchar(2000),
    failure_reason varchar(1000),
    constraint uq_payment_attempt_provider_id unique (provider, provider_payment_id)
);
