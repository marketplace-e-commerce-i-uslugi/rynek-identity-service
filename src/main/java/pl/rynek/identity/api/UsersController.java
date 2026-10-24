package pl.rynek.identity.api;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.rynek.identity.account.AccountService;
import pl.rynek.identity.api.dto.CreateAddressRequest;
import pl.rynek.identity.api.dto.SetPreferencesRequest;
import pl.rynek.identity.api.dto.SetRolesRequest;
import pl.rynek.identity.api.dto.UpdateAddressRequest;
import pl.rynek.identity.api.dto.UpdateMeRequest;
import pl.rynek.identity.api.dto.UserResponse;

import static pl.rynek.identity.common.NotImplemented.notImplemented;

@RestController
public class UsersController {

    private final AccountService accounts;

    public UsersController(AccountService accounts) {
        this.accounts = accounts;
    }

    /**
     * Profil zalogowanego. Dostęp: user.
     */
    @GetMapping("/api/v1/users/me")
    public UserResponse getMe(@AuthenticationPrincipal Jwt token) {
        return UserResponse.of(accounts.get(userId(token)));
    }

    /**
     * Zmiana danych profilu. Dostęp: user.
     */
    @PatchMapping("/api/v1/users/me")
    public UserResponse updateMe(@AuthenticationPrincipal Jwt token, @Valid @RequestBody UpdateMeRequest request) {
        return UserResponse.of(accounts.update(userId(token), request));
    }

    /**
     * Usunięcie konta (RODO). Dostęp: user.
     */
    @DeleteMapping("/api/v1/users/me")
    public void deleteMe() {
        throw notImplemented();
    }

    /**
     * Wybór ról z onboardingu (kupujący / usługodawca / merchant). Dostęp: user.
     */
    @PutMapping("/api/v1/users/me/roles")
    public UserResponse setRoles(@AuthenticationPrincipal Jwt token, @Valid @RequestBody SetRolesRequest request) {
        return UserResponse.of(accounts.chooseRoles(userId(token), request.roles()));
    }

    /**
     * Zainteresowania, lokalizacja, motyw jasny/ciemny. Dostęp: user.
     */
    @PutMapping("/api/v1/users/me/preferences")
    public void setPreferences(@RequestBody SetPreferencesRequest request) {
        throw notImplemented();
    }

    /**
     * Lista adresów. Dostęp: user.
     */
    @GetMapping("/api/v1/users/me/addresses")
    public void listAddresses() {
        throw notImplemented();
    }

    /**
     * Dodanie adresu. Dostęp: user.
     */
    @PostMapping("/api/v1/users/me/addresses")
    public void createAddress(@RequestBody CreateAddressRequest request) {
        throw notImplemented();
    }

    /**
     * Edycja adresu. Dostęp: user.
     */
    @PatchMapping("/api/v1/users/me/addresses/{addressId}")
    public void updateAddress(@PathVariable String addressId, @RequestBody UpdateAddressRequest request) {
        throw notImplemented();
    }

    /**
     * Usunięcie adresu. Dostęp: user.
     */
    @DeleteMapping("/api/v1/users/me/addresses/{addressId}")
    public void deleteAddress(@PathVariable String addressId) {
        throw notImplemented();
    }

    /**
     * Zapisane oferty. Dostęp: user.
     */
    @GetMapping("/api/v1/users/me/saved")
    public void listSavedOffers() {
        throw notImplemented();
    }

    /**
     * Zapisz ofertę. Dostęp: user.
     */
    @PutMapping("/api/v1/users/me/saved/{offerId}")
    public void saveOffer(@PathVariable String offerId) {
        throw notImplemented();
    }

    /**
     * Usuń z zapisanych. Dostęp: user.
     */
    @DeleteMapping("/api/v1/users/me/saved/{offerId}")
    public void unsaveOffer(@PathVariable String offerId) {
        throw notImplemented();
    }

    private static UUID userId(Jwt token) {
        return UUID.fromString(token.getSubject());
    }
}
