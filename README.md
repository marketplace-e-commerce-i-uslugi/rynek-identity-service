# rynek-identity-service

`identity-service` — konta, logowanie, role, profil

> Status: **szkielet**. Wszystkie endpointy z tabeli istnieją w kontrolerach (`src/main/java/.../api/`) i zwracają `501 Not Implemented` (RFC 7807). Ciała żądań to puste rekordy w `api/dto/`.

## Uruchomienie

```bash
./mvnw spring-boot:run   # port 8081, zmienna PORT
./mvnw verify            # testy
```

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

## Do rozstrzygnięcia

Gateway (§2) kieruje ruch po prefiksie ścieżki, a poniższe endpointy leżą pod prefiksem innego serwisu. Możliwe rozwiązania: szczegółowe trasy w gateway przed trasami prefiksowymi albo zmiana ścieżek.

- `/api/v1/users/me/following`: według dokumentu należy do `channel-service`, ale prefiks w gateway kieruje do `identity-service`.
