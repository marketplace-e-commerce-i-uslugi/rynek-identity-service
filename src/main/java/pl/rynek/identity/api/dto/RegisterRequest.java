package pl.rynek.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Ciało żądania: Rejestracja (e-mail/telefon + hasło). Wymagany e-mail albo telefon (sprawdza AccountService).
 * Kontrakt: {@code RegisterRequest} w rynek-frontend/src/app/core/api/models/identity.ts.
 */
public record RegisterRequest(
        @Email(message = "Podaj poprawny adres e-mail.") @Size(max = 254, message = "Adres e-mail jest za długi.") String email,
        String phone,
        // BCrypt uwzględnia tylko pierwsze 72 bajty hasła.
        @NotBlank(message = "Podaj hasło.") @Size(min = 8, max = 72, message = "Hasło musi mieć od 8 do 72 znaków.") String password,
        @NotBlank(message = "Podaj nazwę.") @Size(min = 2, max = 80, message = "Nazwa musi mieć od 2 do 80 znaków.") String displayName) {
}
