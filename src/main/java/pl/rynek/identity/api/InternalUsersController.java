package pl.rynek.identity.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pl.rynek.identity.account.AccountService;
import pl.rynek.identity.api.dto.UserResponse;

@RestController
public class InternalUsersController {

    private final AccountService accounts;

    public InternalUsersController(AccountService accounts) {
        this.accounts = accounts;
    }

    /**
     * Dane użytkownika dla innych serwisów. Dostęp: int.
     */
    @GetMapping("/internal/users/{userId}")
    public UserResponse getUser(@PathVariable String userId) {
        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleźliśmy tego konta.");
        }
        return UserResponse.of(accounts.get(id));
    }
}
