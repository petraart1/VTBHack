--liquibase formatted sql

--changeset Artyom:14
create index idx_tx_category_amount on account_transactions(merchant_category_code, amount desc)
where merchant_category_code is not null;
--rollback drop index if exists idx_tx_category_amount;

--changeset Artyom:15
create index idx_tx_merchant_category_date on account_transactions(merchant_name, merchant_category_code, booking_datetime desc)
where merchant_name is not null and merchant_category_code is not null;
--rollback drop index if exists idx_tx_merchant_category_date;

--changeset Artyom:16
create index idx_tx_amount_range on account_transactions(amount desc, booking_datetime desc)
where amount > 1000;
--rollback drop index if exists idx_tx_amount_range;

--changeset Artyom:17
create index idx_tx_small_amounts on account_transactions(amount asc, booking_datetime desc)
where amount between 10 and 1000;
--rollback drop index if exists idx_tx_small_amounts;

--changeset Artyom:18
create index idx_tx_monthly_stats on account_transactions(
    account_id,
    cast(extract(year from booking_datetime) as integer),
    cast(extract(month from booking_datetime) as integer),
    debit_credit_indicator,
    amount
);
--rollback drop index if exists idx_tx_monthly_stats;

--changeset Artyom:19
create index idx_tx_weekly_stats on account_transactions(
    account_id,
    cast(extract(year from booking_datetime) as integer),
    cast(extract(week from booking_datetime) as integer),
    merchant_category_code
);
--rollback drop index if exists idx_tx_weekly_stats;

--changeset Artyom:20
create index idx_tx_duplicates on account_transactions(
    account_id,
    amount,
    cast(booking_datetime as date),
    description
) where description is not null;
--rollback drop index if exists idx_tx_duplicates;

--changeset Artyom:21
create index idx_tx_currency_operations on account_transactions(
    currency,
    debit_credit_indicator,
    booking_datetime desc
);
--rollback drop index if exists idx_tx_currency_operations;

--changeset Artyom:22
create index idx_tx_regular_payments on account_transactions(
    account_id,
    cast(extract(day from booking_datetime) as integer),
    amount,
    merchant_name
) where merchant_name is not null;
--rollback drop index if exists idx_tx_regular_payments;

--changeset Artyom:23
create index idx_balances_amount_trends on account_balances(
    account_id,
    balance_type,
    amount desc,
    as_of_datetime desc
);
--rollback drop index if exists idx_balances_amount_trends;

--changeset Artyom:24
create index idx_balances_date_range on account_balances(
    account_id,
    as_of_datetime desc,
    balance_type
);
--rollback drop index if exists idx_balances_date_range;

--changeset Artyom:25
create index idx_accounts_status_updates on bank_accounts(
    status,
    updated_at desc,
    user_id
);
--rollback drop index if exists idx_accounts_status_updates;

--changeset Artyom:26
create index idx_accounts_currency on bank_accounts(
    user_id,
    currency,
    status
) where status = 'ACTIVE';
--rollback drop index if exists idx_accounts_currency;

--changeset Artyom:27
create index idx_accounts_sync_priority on bank_accounts(
    last_sync_at asc,
    status
) where status = 'ACTIVE';
--rollback drop index if exists idx_accounts_sync_priority;

--changeset Artyom:28
create index idx_accounts_type on bank_accounts(
    account_type,
    user_id,
    status
) where account_type is not null and status = 'ACTIVE';
--rollback drop index if exists idx_accounts_type;

--changeset Artyom:29
create index idx_sync_performance on sync_logs(
    sync_type,
    duration_ms desc,
    started_at desc
) where duration_ms > 0;
--rollback drop index if exists idx_sync_performance;

--changeset Artyom:30
create index idx_sync_recent_activity on sync_logs(
    user_id,
    started_at desc
);
--rollback drop index if exists idx_sync_recent_activity;
