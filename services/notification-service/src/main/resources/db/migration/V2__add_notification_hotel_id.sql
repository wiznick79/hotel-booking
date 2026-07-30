alter table notifications add column hotel_id varchar(255);

create index idx_notifications_hotel_id on notifications (hotel_id);
