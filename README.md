# rynek-identity-service

`identity-service` — konta, logowanie, role, profil

> Status: **konta i logowanie działają**: rejestracja, logowanie, odświeżanie i wylogowanie, profil (`GET`/`PATCH /users/me`), role z onboardingu, JWKS i `GET /internal/users/{id}`. Pozostałe endpointy z tabeli zwracają `501 Not Implemented` (RFC 7807).

## Uruchomienie

Wymaga PostgreSQL z `rynek-infra/local` (baza `identity`):

```bash
docker compose -f ../rynek-infra/local/docker-compose.yml up -d
./mvnw spring-boot:run   # port 8081, zmienna PORT
./mvnw verify            # testy; Testcontainers uruchamia własny PostgreSQL, wymaga Dockera
```

| Zmienna | Domyślnie | Znaczenie |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/identity`, `identity`, `identity` | Baza serwisu |
| `JWT_PRIVATE_KEY` | pusty | Klucz RSA (PEM, PKCS#8) do podpisu tokenów. Pusty: klucz generowany przy starcie, tylko lokalnie |
| `JWT_ISSUER` | `rynek-identity` | Claim `iss`; musi się zgadzać z gateway |
| `JWT_ACCESS_TOKEN_TTL`, `JWT_REFRESH_TOKEN_TTL` | `15m`, `30d` | Ważność tokenów |
| `JWT_REFRESH_REUSE_GRACE` | `30s` | Czas po rotacji, w którym ponowne użycie starego refresh tokenu nie unieważnia sesji |

Schemat zakłada Flyway (`src/main/resources/db/migration`). Hibernate tylko sprawdza zgodność encji (`ddl-auto: validate`).

- Swagger UI: http://localhost:8081/swagger-ui.html
- Zdrowie: http://localhost:8081/actuator/health

## Zakres i endpointy

_Skopiowane z `rynek-docs/architektura/rynek-endpointy-mikroserwisy.md`. Od teraz to repozytorium jest właścicielem tej specyfikacji._

Ekrany: 1 Onboarding, Profil, każdy ekran wymagający logowania.

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Rejestracja (e-mail/telefon + hasło) | pub |
| POST | `/api/v1/auth/login` | Logowanie, zwraca access + refresh token | pub |
| POST | `/api/v1/auth/oauth/google` | Logowanie przez Google | pub |
| POST | `/api/v1/auth/refresh` | Odświeżenie tokenu | pub |
| POST | `/api/v1/auth/logout` | Unieważnienie refresh tokenu | user |
| POST | `/api/v1/auth/password/forgot` | Prośba o reset hasła | pub |
| POST | `/api/v1/auth/password/reset` | Ustawienie nowego hasła | pub |
| POST | `/api/v1/auth/phone/verify/request` | Wysłanie kodu SMS | user |
| POST | `/api/v1/auth/phone/verify/confirm` | Potwierdzenie kodu | user |
| GET | `/api/v1/users/me` | Profil zalogowanego | user |
| PATCH | `/api/v1/users/me` | Zmiana danych profilu | user |
| DELETE | `/api/v1/users/me` | Usunięcie konta (RODO) | user |
| PUT | `/api/v1/users/me/roles` | Wybór ról z onboardingu (kupujący / usługodawca / merchant) | user |
| PUT | `/api/v1/users/me/preferences` | Zainteresowania, lokalizacja, motyw jasny/ciemny | user |
| GET | `/api/v1/users/me/addresses` | Lista adresów | user |
| POST | `/api/v1/users/me/addresses` | Dodanie adresu | user |
| PATCH | `/api/v1/users/me/addresses/{addressId}` | Edycja adresu | user |
| DELETE | `/api/v1/users/me/addresses/{addressId}` | Usunięcie adresu | user |
| GET | `/api/v1/users/me/saved` | Zapisane oferty | user |
| PUT | `/api/v1/users/me/saved/{offerId}` | Zapisz ofertę | user |
| DELETE | `/api/v1/users/me/saved/{offerId}` | Usuń z zapisanych | user |
| GET | `/internal/users/{userId}` | Dane użytkownika dla innych serwisów | int |
| GET | `/internal/auth/jwks` | Klucze publiczne JWT (JWKS) dla gateway i serwisów | int |

### Token dostępowy

Uzgodnione z `rynek-api-gateway`, które waliduje token:

- JWT podpisany RS256; klucz publiczny w `GET /internal/auth/jwks` (format JWK Set, z `kid`);
- claimy: `iss` = `rynek-identity`, `sub` = id użytkownika, `roles` = lista z `BUYER`, `SERVICE_PROVIDER`, `MERCHANT`, `ADMIN`, `sid` = id sesji, `exp`, `iat`;
- `kid` to odcisk klucza (RFC 7638), więc ten sam klucz ma zawsze ten sam `kid`;
- gateway z `JWT_ENFORCE=true` sprawdza token przez JWKS. identity sprawdza go też sam, więc endpointy `user` są chronione także przy `JWT_ENFORCE=false`.

### Sesje i refresh token

- Każde logowanie lub rejestracja tworzy sesję (tabela `sessions`). Refresh token to losowy ciąg, a w bazie jest tylko jego skrót SHA-256.
- `POST /auth/refresh` rotuje token: stary przestaje działać. Ponowne użycie zrotowanego tokenu (ktoś ma jego kopię) unieważnia całą sesję i zapisuje ostrzeżenie w logu (tabela `used_refresh_tokens`, OAuth 2.0 Security BCP).
- Wyjątek: ponowne użycie w ciągu `JWT_REFRESH_REUSE_GRACE` (30 s) od rotacji to zwykle druga karta przeglądarki. Dostaje 401, ale sesja trwa, a frontend przejmuje tokeny zapisane przez pierwszą kartę. Równoległe odświeżenia tej samej sesji są szeregowane blokadą wiersza (`SELECT ... FOR UPDATE`). Nowy access token ma aktualne role, więc po onboardingu role trafiają do tokenu przy najbliższym odświeżeniu (najpóźniej po 15 min).
- `POST /auth/logout` kończy tylko bieżącą sesję (`sid` z tokenu). Wydany access token działa do wygaśnięcia, bo nikt nie sprawdza go w bazie.
- Na endpointach `pub` i `/internal/**` nagłówek `Authorization` jest ignorowany, więc wygasły access token nie blokuje logowania ani `POST /auth/refresh`.
- Login to e-mail (zapisywany małymi literami) albo telefon (E.164; 9 cyfr bez prefiksu = `+48`). Złe hasło i nieznany login dają tę samą odpowiedź `401`.

### Do zrobienia

- `user.registered` i `user.roles-changed` (Kafka): jeszcze nie publikowane, bo projekt nie ma Kafki.
- `channelId`/`channelHandle` w profilu są puste, dopóki channel-service nie wystawia kanałów.
- Google, reset hasła i SMS czekają na dostawców (§ Dane i zależności).

## Zdarzenia Kafka

Wiersze z §17, w których występuje `identity` (publikuje lub słucha).

| Zdarzenie | Publikuje | Słuchają |
|---|---|---|
| `user.registered`, `user.roles-changed` | identity | channel, notification |

## Dane i zależności

- **Magazyn danych:** PostgreSQL
- **Zależności:** Google OAuth, bramka SMS, dostawca e-mail
- **Kontrakty:** `rynek-contracts/openapi/` i `rynek-contracts/events/`
- **Lokalne uruchomienie:** `rynek-infra/local/`

## Kolejność budowy

Etap z §19: 1. `api-gateway`, `identity-service`, `channel-service` — konta i kanały.

## Wyjątki routingu

Gateway (§2) kieruje ruch po prefiksie ścieżki, a poniższe endpointy leżą pod prefiksem innego serwisu. Gateway obsługuje je trasami szczegółowymi (`order: -1`), które mają pierwszeństwo przed trasami po prefiksie. Ścieżki pozostają bez zmian (decyzja z 2026-10-24). Test `RoutingTests` w `rynek-api-gateway` sprawdza, czy każda z nich trafia do właściwego serwisu.

- `/api/v1/users/me/following`: należy do `channel-service` (prefiks wskazuje `identity-service`).
