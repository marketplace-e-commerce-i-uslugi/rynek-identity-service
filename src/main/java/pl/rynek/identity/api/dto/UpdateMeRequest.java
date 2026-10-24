package pl.rynek.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Ciało żądania: Zmiana danych profilu. Pola pominięte (null) zostają bez zmian.
 */
public record UpdateMeRequest(
        @Size(min = 2, max = 80, message = "Nazwa musi mieć od 2 do 80 znaków.") String displayName,
        @Email(message = "Podaj poprawny adres e-mail.") @Size(max = 254, message = "Adres e-mail jest za długi.") String email,
        String phone) {
}
