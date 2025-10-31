-- liquibase formatted sql

--changeset Artyom:1
create table users (
    id bigserial primary key,
    username varchar(50) not null unique,
    email varchar(100) not null unique,
    password varchar(100) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    is_active boolean not null default true
);
--rollback drop table users

--changeset Artyom:2
create index idx_users_username on users(username);
create index idx_users_email on users(email);
create index idx_users_is_active on users (is_active);
create index idx_users_created_at on users (created_at desc);
create index idx_users_updated_at on users (updated_at desc);

-- rollback drop index if exists idx_users_updated_at;
-- rollback drop index if exists idx_users_created_at;
-- rollback drop index if exists idx_users_is_active;
-- rollback drop index if exists idx_users_email;
-- rollback drop index if exists idx_users_username;