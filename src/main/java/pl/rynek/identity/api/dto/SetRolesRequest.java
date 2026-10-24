package pl.rynek.identity.api.dto;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import pl.rynek.identity.account.Role;

/**
 * Ciało żądania: Wybór ról z onboardingu (kupujący / usługodawca / merchant).
 */
public record SetRolesRequest(@NotEmpty(message = "Wybierz co najmniej jedną rolę.") Set<Role> roles) {
}
