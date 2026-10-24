package pl.rynek.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Ciało żądania: Odświeżenie tokenu.
 */
public record RefreshTokenRequest(@NotBlank(message = "Brak refresh tokenu.") String refreshToken) {
}
