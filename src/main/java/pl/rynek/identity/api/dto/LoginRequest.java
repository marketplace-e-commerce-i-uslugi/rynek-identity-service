package pl.rynek.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Ciało żądania: Logowanie, zwraca access + refresh token. {@code login} to e-mail albo numer telefonu.
 */
public record LoginRequest(
        @NotBlank(message = "Podaj e-mail lub numer telefonu.") String login,
        @NotBlank(message = "Podaj hasło.") String password) {
}
