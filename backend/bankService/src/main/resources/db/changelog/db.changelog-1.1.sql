-- liquibase formatted sql

-- changeset Artyom:13
alter bank_accounts add column version bigint not null default 0;

create index idx_bank_accounts_version on bank_accounts(version);

-- rollback drop index if exists idx_bank_accounts_version;
-- rollback alter table bank_accounts drop column if exists version;