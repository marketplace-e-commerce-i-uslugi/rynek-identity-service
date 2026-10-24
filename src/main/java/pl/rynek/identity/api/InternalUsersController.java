package pl.rynek.identity.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static pl.rynek.identity.common.NotImplemented.notImplemented;

@RestController
public class InternalUsersController {

    /**
     * Dane użytkownika dla innych serwisów. Dostęp: int.
     */
    @GetMapping("/internal/users/{userId}")
    public void getUser(@PathVariable String userId) {
        throw notImplemented();
    }
}
