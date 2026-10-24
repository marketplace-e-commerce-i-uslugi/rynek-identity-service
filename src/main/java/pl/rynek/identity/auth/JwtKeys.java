package pl.rynek.identity.auth;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Klucz RSA do podpisywania access tokenów (RS256). Publiczna część trafia do JWKS ({@code GET /internal/auth/jwks}),
 * a {@code kid} to odcisk klucza (RFC 7638), więc ten sam klucz ma zawsze ten sam {@code kid}.
 */
public final class JwtKeys {

    private static final Logger log = LoggerFactory.getLogger(JwtKeys.class);

    private final RSAKey key;

    private JwtKeys(RSAKey key) {
        this.key = key;
    }

    /** Klucz z konfiguracji (PEM, PKCS#8) albo, gdy pusty, nowy klucz na czas działania procesu. */
    public static JwtKeys load(String privateKeyPem) {
        try {
            if (privateKeyPem == null || privateKeyPem.isBlank()) {
                log.warn("Brak rynek.jwt.private-key (JWT_PRIVATE_KEY): klucz JWT wygenerowany przy starcie. "
                        + "Po restarcie access tokeny tracą ważność. Tylko do pracy lokalnej.");
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                return of((RSAPrivateCrtKey) generator.generateKeyPair().getPrivate());
            }
            String base64 = privateKeyPem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return of((RSAPrivateCrtKey) factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64))));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException | ClassCastException e) {
            throw new IllegalStateException("Nieprawidłowy klucz rynek.jwt.private-key: oczekiwany klucz RSA w PEM (PKCS#8)", e);
        }
    }

    private static JwtKeys of(RSAPrivateCrtKey privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
        try {
            return new JwtKeys(new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyIDFromThumbprint()
                    .build());
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    public RSAKey signingKey() {
        return key;
    }

    public RSAPublicKey publicKey() {
        try {
            return key.toRSAPublicKey();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    public RSAKey publicJwk() {
        return key.toPublicJWK();
    }
}
