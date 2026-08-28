alter table notifications
    add column reference_id uuid;

update notifications
    set reference_id = reservation_id;

alter table notifications
    alter column reference_id set not null;

alter table notifications
    alter column reservation_id drop not null;

drop index uq_notification_reservation_subject;

create unique index uq_notification_reference_subject
    on notifications (reference_id, subject);
