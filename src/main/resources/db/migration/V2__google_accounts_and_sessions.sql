ALTER TABLE accounts
    ADD COLUMN google_sub VARCHAR(255) NOT NULL,
    ADD COLUMN display_name VARCHAR(320) NOT NULL,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ALTER COLUMN password_hash DROP NOT NULL,
    ALTER COLUMN role SET DEFAULT 'USER';

CREATE UNIQUE INDEX accounts_google_sub_uidx ON accounts (google_sub);

CREATE TABLE sessions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX sessions_account_id_idx ON sessions (account_id);
