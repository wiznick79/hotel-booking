alter table notifications
    add column sender_display_name varchar(255),
    add column sender_from_address varchar(320),
    add column sender_reply_to_address varchar(320);
