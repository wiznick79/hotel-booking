drop table if exists rate_periods;

create table pricing_rules (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone,
    version integer not null,
    active boolean not null,
    erased boolean not null,
    hotel_id uuid not null references hotels(id),
    name varchar(255) not null,
    rule_type varchar(30) not null,
    recurring_start_month integer,
    recurring_start_day integer,
    recurring_end_month integer,
    recurring_end_day integer,
    start_date date,
    end_date date,
    priority integer not null
);

create table pricing_rule_room_type_prices (
    id uuid primary key,
    pricing_rule_id uuid not null references pricing_rules(id) on delete cascade,
    room_type_id uuid not null references room_types(id),
    normal_nightly_price numeric(19, 2) not null,
    weekend_nightly_price numeric(19, 2),
    constraint uq_pricing_rule_room_type unique (pricing_rule_id, room_type_id)
);
