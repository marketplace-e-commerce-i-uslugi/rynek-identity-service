package pl.rynek.identity.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static pl.rynek.identity.common.NotImplemented.notImplemented;

@RestController
public class InternalAuthController {

    /**
     * Klucze publiczne (JWKS) do walidacji JWT w gateway i serwisach. Dostęp: int.
     */
    @GetMapping("/internal/auth/jwks")
    public void getJwks() {
        throw notImplemented();
    }
}
