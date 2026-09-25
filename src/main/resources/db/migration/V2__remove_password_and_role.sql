DROP INDEX IF EXISTS accounts_role_idx;

ALTER TABLE accounts
    DROP COLUMN IF EXISTS password_hash,
    DROP COLUMN IF EXISTS role;
