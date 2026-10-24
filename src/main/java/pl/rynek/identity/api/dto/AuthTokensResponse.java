package pl.rynek.identity.api.dto;

/**
 * Para tokenów po logowaniu, rejestracji i odświeżeniu. {@code expiresIn}: ważność access tokenu w sekundach.
 * Kontrakt: {@code AuthTokens} w rynek-frontend/src/app/core/api/models/identity.ts.
 */
public record AuthTokensResponse(String accessToken, String refreshToken, long expiresIn) {
}
