create table identity_refresh_tokens (
    id uuid primary key,
    token_hash varchar(64) not null unique,
    user_id bigint not null,
    expires_at timestamp with time zone not null,
    revoked_at timestamp with time zone,
    created_at timestamp with time zone not null,
    constraint fk_identity_refresh_tokens_user
        foreign key (user_id) references identity_users(id)
);

create index idx_identity_refresh_tokens_user_id on identity_refresh_tokens(user_id);
