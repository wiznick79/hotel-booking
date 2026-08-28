alter table reservations
    add column payment_method varchar(30) not null default 'PAY_AT_RECEPTION';
