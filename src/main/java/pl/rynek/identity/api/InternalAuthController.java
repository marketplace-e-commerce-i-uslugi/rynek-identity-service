package pl.rynek.identity.api;

import java.util.Map;

import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.rynek.identity.auth.JwtKeys;

@RestController
public class InternalAuthController {

    private final JwtKeys keys;

    public InternalAuthController(JwtKeys keys) {
        this.keys = keys;
    }

    /**
     * Klucze publiczne (JWKS) do walidacji JWT w gateway i serwisach. Dostęp: int.
     */
    @GetMapping("/internal/auth/jwks")
    public Map<String, Object> getJwks() {
        return new JWKSet(keys.publicJwk()).toJSONObject();
    }
}
