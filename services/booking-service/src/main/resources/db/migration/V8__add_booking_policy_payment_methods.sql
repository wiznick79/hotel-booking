create table booking_policy_payment_methods (
    booking_policy_id uuid not null references booking_policies(id) on delete cascade,
    payment_method varchar(30) not null,
    primary key (booking_policy_id, payment_method)
);
