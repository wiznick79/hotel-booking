alter table notifications
    add column notification_type varchar(40),
    add column contact_name varchar(120),
    add column contact_email varchar(180),
    add column contact_phone varchar(40),
    add column contact_message text,
    add column created_at timestamp with time zone not null default current_timestamp,
    add column read_at timestamp with time zone;

update notifications
set notification_type = case
        when reservation_id is not null then 'RESERVATION'
        when hotel_id is null then 'ACCOUNT_VERIFICATION'
        else 'CONTACT_MESSAGE'
    end;

update notifications
set created_at = coalesce(last_attempt_at, created_at);

update notifications
set contact_name = split_part(split_part(body, 'Name: ', 2), E'\n', 1),
    contact_email = split_part(split_part(body, 'Email: ', 2), E'\n', 1),
    contact_phone = nullif(split_part(split_part(body, 'Phone: ', 2), E'\n', 1), 'Not provided'),
    contact_message = split_part(body, E'\n\n', 3)
where notification_type = 'CONTACT_MESSAGE';

alter table notifications
    alter column notification_type set not null;

create index idx_notifications_contact_inbox
    on notifications (hotel_id, notification_type, created_at desc);
