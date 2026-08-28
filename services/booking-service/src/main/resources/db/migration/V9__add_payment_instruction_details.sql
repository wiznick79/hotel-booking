alter table payment_attempts
    add column provider_payment_intent_id varchar(255),
    add column payment_entity varchar(255),
    add column payment_reference varchar(255),
    add column hosted_voucher_url varchar(2000),
    add column payment_instructions_expire_at timestamp with time zone;

create unique index uq_payment_attempt_provider_intent_id
    on payment_attempts (provider, provider_payment_intent_id)
    where provider_payment_intent_id is not null;
