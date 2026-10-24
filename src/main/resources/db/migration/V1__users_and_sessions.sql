CREATE TABLE users (
    id             uuid PRIMARY KEY,
    -- E-mail zapisany małymi literami, telefon w formacie E.164 (+48...). Wymagany co najmniej jeden.
    email          varchar(254) UNIQUE,
    phone          varchar(16) UNIQUE,
    phone_verified boolean      NOT NULL DEFAULT false,
    password_hash  varchar(100) NOT NULL,
    display_name   varchar(80)  NOT NULL,
    avatar_url     varchar(500),
    onboarded      boolean      NOT NULL DEFAULT false,
    created_at     timestamptz  NOT NULL,
    updated_at     timestamptz  NOT NULL,
    CONSTRAINT users_email_or_phone CHECK (email IS NOT NULL OR phone IS NOT NULL)
);

CREATE TABLE user_roles (
    user_id uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    varchar(20) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- Sesja = jedno zalogowanie na urządzeniu. Refresh token jest rotowany przy każdym odświeżeniu;
-- w bazie tylko jego skrót SHA-256.
CREATE TABLE sessions (
    id                 uuid PRIMARY KEY,
    user_id            uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    refresh_token_hash varchar(64) NOT NULL UNIQUE,
    created_at         timestamptz NOT NULL,
    expires_at         timestamptz NOT NULL,
    revoked_at         timestamptz
);

CREATE INDEX sessions_user_id_idx ON sessions (user_id);
