-- Skróty refresh tokenów zużytych przy rotacji. Ponowne użycie takiego tokenu oznacza, że ktoś go skopiował:
-- sesja zostaje unieważniona (OAuth 2.0 Security BCP, wykrywanie ponownego użycia).
CREATE TABLE used_refresh_tokens (
    token_hash varchar(64) PRIMARY KEY,
    session_id uuid        NOT NULL REFERENCES sessions (id) ON DELETE CASCADE,
    used_at    timestamptz NOT NULL
);

CREATE INDEX used_refresh_tokens_session_id_idx ON used_refresh_tokens (session_id);
