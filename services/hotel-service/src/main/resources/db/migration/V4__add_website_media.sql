create table website_media (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone,
    version integer not null,
    active boolean not null,
    erased boolean not null,
    hotel_id uuid not null references hotels(id),
    room_type_id uuid references room_types(id),
    usage varchar(40) not null,
    sort_order integer not null,
    alt_text varchar(255),
    original_filename varchar(255) not null,
    storage_key varchar(255) not null unique,
    content_type varchar(50) not null,
    size_bytes bigint not null,
    constraint ck_website_media_owner check (
        (usage = 'ROOM_TYPE_GALLERY' and room_type_id is not null)
        or (usage <> 'ROOM_TYPE_GALLERY' and room_type_id is null)
    )
);

create index ix_website_media_hotel on website_media(hotel_id, usage, sort_order);
create index ix_website_media_room_type on website_media(room_type_id, sort_order);
