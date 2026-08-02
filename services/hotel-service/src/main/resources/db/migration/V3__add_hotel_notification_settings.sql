alter table hotels
    add column notification_display_name varchar(255),
    add column notification_from_address varchar(320),
    add column notification_reply_to_address varchar(320);
