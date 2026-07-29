create table identity_user_hotels (
    user_id bigint not null,
    hotel_id uuid not null,
    primary key (user_id, hotel_id),
    constraint fk_identity_user_hotels_user
        foreign key (user_id) references identity_users(id)
);
