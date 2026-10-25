-- Kanał konta z channel-service (PUT /internal/users/{id}/channel). Jedno konto ma najwyżej jeden kanał.
ALTER TABLE users
    ADD COLUMN channel_id     uuid UNIQUE,
    ADD COLUMN channel_handle varchar(30);
