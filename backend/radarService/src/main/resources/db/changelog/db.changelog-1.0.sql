-- liquibase formatted sql

--changeset Artyom:1
create extension if not exists "uuid-ossp";
create extension if not exists "pg_trgm";

--rollback drop extension if exists "pg_trgm";
--rollback drop extension if exists "uuid-ossp";

--changeset Artyom:2
create table subscriptions (
    id uuid primary key default uuid_generate_v4(),
    user_id uuid not null,
    account_id uuid,
    merchant_name varchar(256) not null,
    category varchar(64),
    amount numeric(18,2) not null,
    currency varchar(3) not null,
    frequency varchar(32) not null,
    status varchar(32) not null default 'DETECTED',
    first_payment_date timestamp not null,
    last_payment_date timestamp not null,
    next_expected_date timestamp,
    total_payments integer not null default 0,
    total_spent numeric(18,2) not null default 0,
    price_increased boolean default false,
    previous_amount numeric(18,2),
    confidence_score numeric(3,2) check (confidence_score >= 0 and confidence_score <= 1),
    description text,
    cancellation_url varchar(512),
    cancellation_instructions text,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

--rollback drop table if exists subscriptions cascade;

--changeset Artyom:3
create table price_history (
    id uuid primary key default uuid_generate_v4(),
    subscription_id uuid not null references subscriptions(id) on delete cascade,
    old_price numeric(18,2) not null,
    new_price numeric(18,2) not null,
    price_change numeric(18,2) not null,
    percentage_change numeric(5,2),
    transaction_id uuid,
    detected_at timestamp not null default current_timestamp
);

--rollback drop table if exists price_history cascade;

--changeset Artyom:4
create table subscription_alerts (
    id uuid primary key default uuid_generate_v4(),
    user_id uuid not null,
    subscription_id uuid references subscriptions(id) on delete cascade,
    alert_type varchar(64) not null,
    title varchar(256) not null,
    message text not null,
    action_url varchar(512),
    is_read boolean not null default false,
    created_at timestamp not null default current_timestamp
);

--rollback drop table if exists subscription_alerts cascade;

--changeset Artyom:5
create table subscription_alternatives (
    id uuid primary key default uuid_generate_v4(),
    subscription_id uuid not null references subscriptions(id) on delete cascade,
    alternative_name varchar(256) not null,
    description text,
    price numeric(18,2) not null,
    currency varchar(3) not null,
    frequency varchar(32) not null,
    savings numeric(18,2),
    referral_link varchar(512),
    rating integer check (rating >= 1 and rating <= 5),
    created_at timestamp not null default current_timestamp
);

--rollback drop table if exists subscription_alternatives cascade;

--changeset Artyom:6
create table transaction_patterns (
    id uuid primary key default uuid_generate_v4(),
    user_id uuid not null,
    merchant_name varchar(256) not null,
    merchant_category varchar(64),
    occurrence_count integer not null default 0,
    average_amount numeric(18,2),
    std_deviation numeric(18,2),
    average_days_between integer,
    std_dev_days integer,
    recent_payment_dates text,
    first_seen timestamp not null,
    last_seen timestamp not null,
    last_analyzed timestamp not null default current_timestamp,
    constraint uc_user_merchant unique(user_id, merchant_name)
);

--rollback drop table if exists transaction_patterns cascade;

--changeset Artyom:7
create index idx_subscriptions_user_id on subscriptions(user_id);
create index idx_subscriptions_status on subscriptions(status);
create index idx_subscriptions_next_payment on subscriptions(user_id, next_expected_date);
create index idx_subscriptions_category on subscriptions(user_id, category);
create index idx_subscriptions_merchant on subscriptions(merchant_name);

--rollback drop index if exists idx_subscriptions_merchant;
--rollback drop index if exists idx_subscriptions_category;
--rollback drop index if exists idx_subscriptions_next_payment;
--rollback drop index if exists idx_subscriptions_status;
--rollback drop index if exists idx_subscriptions_user_id;

--changeset Artyom:8
create index idx_price_history_subscription on price_history(subscription_id, detected_at desc);

--rollback drop index if exists idx_price_history_subscription;

--changeset Artyom:9
create index idx_alerts_user_unread on subscription_alerts(user_id, is_read, created_at desc);

--rollback drop index if exists idx_alerts_user_unread;

--changeset Artyom:10
create index idx_alternatives_subscription on subscription_alternatives(subscription_id);

--rollback drop index if exists idx_alternatives_subscription;

--changeset Artyom:11
create index idx_patterns_user on transaction_patterns(user_id, last_analyzed);
create index idx_patterns_merchant on transaction_patterns(merchant_name);

--rollback drop index if exists idx_patterns_merchant;
--rollback drop index if exists idx_patterns_user;

--changeset Artyom:12
alter table subscriptions
    add constraint check_currency_subscription check (currency ~ '^[A-Z]{3}$');

alter table subscription_alternatives
    add constraint check_currency_alternative check (currency ~ '^[A-Z]{3}$');

--rollback alter table subscription_alternatives drop constraint check_currency_alternative;
--rollback alter table subscriptions drop constraint check_currency_subscription;

