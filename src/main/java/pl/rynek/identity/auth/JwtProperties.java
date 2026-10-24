package pl.rynek.identity.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code rynek.jwt.*} z application.yml. */
@ConfigurationProperties("rynek.jwt")
public record JwtProperties(String issuer, String privateKey, Duration accessTokenTtl, Duration refreshTokenTtl,
                            Duration refreshReuseGrace) {
}
