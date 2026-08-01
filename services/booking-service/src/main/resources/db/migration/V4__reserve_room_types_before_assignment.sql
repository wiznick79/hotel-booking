alter table reservation_items add column room_type_id varchar(255);
alter table reservation_items alter column room_id drop not null;
