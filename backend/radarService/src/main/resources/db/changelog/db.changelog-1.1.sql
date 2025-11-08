-- liquibase formatted sql

--changeset Artyom:13
-- Fix column types for Double fields
alter table price_history alter column percentage_change type double precision;
alter table subscriptions alter column confidence_score type double precision;

--rollback alter table price_history alter column percentage_change type numeric(5,2);
--rollback alter table subscriptions alter column confidence_score type numeric(3,2);

