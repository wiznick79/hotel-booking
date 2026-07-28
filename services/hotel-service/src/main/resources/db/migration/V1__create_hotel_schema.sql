-- Draft initial schema for a fresh PostgreSQL hotel-service database.
create table hotels (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone,
    version integer not null,
    active boolean not null,
    erased boolean not null,
    name varchar(255) not null,
    description varchar(2000),
    address varchar(500),
    city varchar(255),
    country varchar(255),
    default_language varchar(10) not null
);

create table room_types (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone,
    version integer not null,
    active boolean not null,
    erased boolean not null,
    hotel_id uuid not null references hotels(id),
    maximum_occupancy integer not null,
    base_price numeric(19, 2) not null
);

create table room_type_translations (
    id uuid primary key,
    room_type_id uuid not null references room_types(id),
    language_code varchar(10) not null,
    name varchar(255) not null,
    description varchar(2000),
    constraint uq_room_type_translation unique (room_type_id, language_code)
);

create table rooms (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone,
    version integer not null,
    active boolean not null,
    erased boolean not null,
    hotel_id uuid not null references hotels(id),
    room_type_id uuid not null references room_types(id),
    room_number varchar(50) not null,
    floor integer,
    status varchar(30) not null,
    constraint uq_room_number unique (hotel_id, room_number)
);

create table rate_periods (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone,
    version integer not null,
    active boolean not null,
    erased boolean not null,
    room_type_id uuid not null references room_types(id),
    start_date date not null,
    end_date date not null,
    normal_nightly_price numeric(19, 2) not null,
    weekend_nightly_price numeric(19, 2),
    name varchar(255)
);

create table room_unavailabilities (
    id uuid primary key,
    room_id uuid not null references rooms(id),
    from_date date not null,
    to_date date not null,
    reason varchar(500),
    emergency boolean not null
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
