-- liquibase formatted sql

-- changeset Artyom:14
create index idx_tx_category_amount on account_transactions(merchant_category_code, amount desc)
where merchant_category_code is not null;

create index idx_tx_merchant_category_date on account_transactions(merchant_name, merchant_category_code, booking_datetime desc)
where merchant_name is not null and merchant_category_code is not null;

create index idx_tx_amount_range on account_transactions(amount desc, booking_datetime desc)
where amount > 1000;

create index idx_tx_small_amounts on account_transactions(amount asc, booking_datetime desc)
where amount between 10 and 1000;

create index idx_tx_monthly_stats on account_transactions(
    account_id,
    extract(year from booking_datetime),
    extract(month from booking_datetime),
    debit_credit_indicator,
    amount
);

create index idx_tx_weekly_stats on account_transactions(
    account_id,
    extract(year from booking_datetime),
    extract(week from booking_datetime),
    merchant_category_code
);

create index idx_tx_duplicates on account_transactions(
    account_id,
    amount,
    booking_datetime::date,
    description
) where description is not null;

create index idx_tx_currency_operations on account_transactions(
    currency,
    debit_credit_indicator,
    booking_datetime desc
);

create index idx_tx_regular_payments on account_transactions(
    account_id,
    extract(day from booking_datetime),
    amount,
    merchant_name
) where merchant_name is not null;

create index idx_balances_amount_trends on account_balances(
    account_id,
    balance_type,
    amount desc,
    as_of_datetime desc
);

create index idx_balances_date_range on account_balances(
    account_id,
    as_of_datetime desc,
    balance_type
) where as_of_datetime > current_timestamp - interval '6 months';

create index idx_accounts_status_updates on bank_accounts(
    status,
    updated_at desc,
    user_id
);

create index idx_accounts_currency on bank_accounts(
    user_id,
    currency,
    status
) where status = 'ACTIVE';

create index idx_accounts_sync_priority on bank_accounts(
    last_sync_at asc,
    status
) where status = 'ACTIVE' and last_sync_at < current_timestamp - interval '1 hour';

create index idx_accounts_type on bank_accounts(
    account_type,
    user_id,
    status
) where account_type is not null and status = 'ACTIVE';

create index idx_sync_performance on sync_logs(
    sync_type,
    duration_ms desc,
    started_at desc
) where duration_ms > 0;

create index idx_sync_recent_activity on sync_logs(
    user_id,
    started_at desc
) where started_at > current_timestamp - interval '7 days';

-- rollback statements
-- rollback drop index if exists idx_sync_recent_activity;
-- rollback drop index if exists idx_sync_performance;
-- rollback drop index if exists idx_accounts_type;
-- rollback drop index if exists idx_accounts_sync_priority;
-- rollback drop index if exists idx_accounts_currency;
-- rollback drop index if exists idx_accounts_status_updates;
-- rollback drop index if exists idx_balances_date_range;
-- rollback drop index if exists idx_balances_amount_trends;
-- rollback drop index if exists idx_tx_regular_payments;
-- rollback drop index if exists idx_tx_currency_operations;
-- rollback drop index if exists idx_tx_duplicates;
-- rollback drop index if exists idx_tx_weekly_stats;
-- rollback drop index if exists idx_tx_monthly_stats;
-- rollback drop index if exists idx_tx_small_amounts;
-- rollback drop index if exists idx_tx_amount_range;
-- rollback drop index if exists idx_tx_merchant_category_date;
-- rollback drop index if exists idx_tx_category_amount;
