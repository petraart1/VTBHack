-- liquibase formatted sql

--changeset Artyom:1
create extension if not exists "uuid-ossp";
create extension if not exists "pg_trgm";

--rollback drop extension if exists "pg_trgm";
--rollback drop extension if exists "uuid-ossp";

--changeset Artyom:2
create table bank_accounts (
    id uuid primary key default uuid_generate_v4(),
    user_id uuid not null,
    bank_id varchar(64) not null,
    external_account_id varchar(128) not null,
    account_number_masked varchar(64),
    iban varchar(34),
    account_type varchar(64),
    currency varchar(3) not null,
    nickname varchar(128),
    available_balance numeric(18,2),
    booked_balance numeric(18,2),
    credit_limit numeric(18,2),
    status varchar(32) not null default 'ACTIVE',
    last_sync_at timestamp,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uc_user_bank_account unique(user_id, bank_id, external_account_id)
);

--rollback drop table if exists bank_accounts cascade;

--changeset Artyom:3
create table account_balances (
    id uuid primary key default uuid_generate_v4(),
    account_id uuid not null references bank_accounts(id) on delete cascade,
    balance_type varchar(64) not null,
    amount numeric(18,2) not null,
    currency varchar(3) not null,
    credit_line numeric(18,2),
    as_of_datetime timestamp not null,
    created_at timestamp not null default current_timestamp,
    constraint uc_balance_snapshot unique(account_id, balance_type, as_of_datetime)
);

--rollback drop table if exists account_balances cascade;

--changeset Artyom:4
create table account_transactions (
    id uuid primary key default uuid_generate_v4(),
    account_id uuid not null references bank_accounts(id) on delete cascade,
    external_transaction_id varchar(128) not null,
    booking_datetime timestamp,
    value_datetime timestamp,
    amount numeric(18,2) not null,
    currency varchar(3) not null,
    debit_credit_indicator varchar(16),
    status varchar(32) not null default 'BOOKED',
    description text,
    merchant_name varchar(256),
    merchant_category_code varchar(64),
    bank_transaction_code varchar(64),
    proprietary_code varchar(64),
    running_balance numeric(18,2),
    created_at timestamp not null default current_timestamp,
    constraint uc_transaction unique(account_id, external_transaction_id)
);

--rollback drop table if exists account_transactions cascade;

--changeset Artyom:5
create table sync_logs (
    id uuid primary key default uuid_generate_v4(),
    user_id uuid not null,
    account_id uuid,
    sync_type varchar(32) not null,
    status varchar(32) not null,
    message text,
    records_fetched integer not null default 0,
    started_at timestamp not null default current_timestamp,
    finished_at timestamp,
    duration_ms bigint
);

--rollback drop table if exists sync_logs cascade;

--changeset Artyom:6
create index idx_accounts_user_id on bank_accounts(user_id);
create index idx_accounts_user_bank on bank_accounts(user_id, bank_id);
create index idx_accounts_external_id on bank_accounts(external_account_id);
create index idx_accounts_last_sync on bank_accounts(last_sync_at);
create index idx_accounts_active on bank_accounts(user_id) where status = 'ACTIVE';

--rollback drop index if exists idx_accounts_active;
--rollback drop index if exists idx_accounts_last_sync;
--rollback drop index if exists idx_accounts_external_id;
--rollback drop index if exists idx_accounts_user_bank;
--rollback drop index if exists idx_accounts_user_id;

--changeset Artyom:7
create index idx_balances_account_time on account_balances(account_id, as_of_datetime desc);
create index idx_balances_recent on account_balances(account_id, balance_type, as_of_datetime desc);
 

--rollback drop index if exists idx_balances_recent;
--rollback drop index if exists idx_balances_account_time;

--changeset Artyom:8
create index idx_tx_account_booking on account_transactions(account_id, booking_datetime desc);
create index idx_tx_merchant on account_transactions(merchant_name, account_id, booking_datetime desc) 
where merchant_name is not null;
create index idx_tx_external_id on account_transactions(external_transaction_id);
create index idx_tx_debits on account_transactions(account_id, booking_datetime desc, merchant_name) 
where debit_credit_indicator = 'DEBIT';

--rollback drop index if exists idx_tx_debits;
--rollback drop index if exists idx_tx_external_id;
--rollback drop index if exists idx_tx_merchant;
--rollback drop index if exists idx_tx_account_booking;

--changeset Artyom:9
create index idx_sync_user_time on sync_logs(user_id, started_at desc);
create index idx_sync_failed on sync_logs(user_id, sync_type, started_at desc) where status = 'FAILED';
create index idx_sync_status on sync_logs(status);

--rollback drop index if exists idx_sync_status;
--rollback drop index if exists idx_sync_failed;
--rollback drop index if exists idx_sync_user_time;

--changeset Artyom:10
alter table bank_accounts
    add constraint check_currency_account check (currency ~ '^[A-Z]{3}$');

alter table account_balances
    add constraint check_currency_balance check (currency ~ '^[A-Z]{3}$');

alter table account_transactions
    add constraint check_currency_tx check (currency ~ '^[A-Z]{3}$');

alter table account_transactions
    add constraint check_debit_credit check (
        debit_credit_indicator is null or debit_credit_indicator in ('DEBIT', 'CREDIT')
        );

alter table account_transactions
    add constraint check_transaction_dates check (
        booking_datetime <= current_timestamp + interval '1 day' and
        (value_datetime is null or value_datetime <= current_timestamp + interval '1 day')
        );

--rollback alter table account_transactions drop constraint check_transaction_dates;
--rollback alter table account_transactions drop constraint check_debit_credit;
--rollback alter table account_transactions drop constraint check_currency_tx;
--rollback alter table account_balances drop constraint check_currency_balance;
--rollback alter table bank_accounts drop constraint check_currency_account;

--changeset Artyom:11
alter table account_transactions set (
    autovacuum_vacuum_scale_factor = 0.0,
    autovacuum_vacuum_threshold = 5000,
    autovacuum_analyze_scale_factor = 0.0,
    autovacuum_analyze_threshold = 5000
    );

--rollback alter table account_transactions reset autovacuum_vacuum_scale_factor;
--rollback alter table account_transactions reset autovacuum_vacuum_threshold;
--rollback alter table account_transactions reset autovacuum_analyze_scale_factor;
--rollback alter table account_transactions reset autovacuum_analyze_threshold;

--changeset Artyom:12
create materialized view daily_account_stats as
select
    account_id,
    date(booking_datetime) as transaction_date,
    count(*) as transaction_count,
    sum(case when debit_credit_indicator = 'DEBIT' then abs(amount) else 0 end) as total_debits,
    sum(case when debit_credit_indicator = 'CREDIT' then amount else 0 end) as total_credits,
    sum(amount) as net_flow
from account_transactions
where booking_datetime > current_timestamp - interval '1 year'
group by account_id, date(booking_datetime);

create unique index idx_daily_stats_account_date on daily_account_stats(account_id, transaction_date desc);

--rollback drop materialized view if exists daily_account_stats;

