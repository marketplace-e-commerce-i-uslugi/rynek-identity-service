package pl.rynek.identity.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.rynek.identity.account.AccountService;
import pl.rynek.identity.account.Contacts;
import pl.rynek.identity.account.UserAccount;
import pl.rynek.identity.account.UserAccountRepository;
import pl.rynek.identity.api.dto.AuthTokensResponse;

/**
 * Logowanie i sesje. Access token: JWT RS256 ({@code iss}, {@code sub}, {@code roles}, {@code sid}, {@code iat}, {@code exp}),
 * krótko ważny i niesprawdzany w bazie. Refresh token: losowy, w bazie tylko jego skrót, rotowany przy każdym odświeżeniu.
 * Ponowne użycie zrotowanego tokenu unieważnia całą sesję (ktoś ma kopię tokenu).
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserAccountRepository users;
    private final AccountService accounts;
    private final AuthSessionRepository sessions;
    private final UsedRefreshTokenRepository usedTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;
    /** Porównanie z tym skrótem, gdy loginu nie ma w bazie: odpowiedź trwa tyle samo co przy złym haśle. */
    private final String dummyPasswordHash;

    public AuthService(UserAccountRepository users, AccountService accounts, AuthSessionRepository sessions,
                       UsedRefreshTokenRepository usedTokens, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                       JwtProperties properties, Clock clock) {
        this.users = users;
        this.accounts = accounts;
        this.sessions = sessions;
        this.usedTokens = usedTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AuthTokensResponse login(String login, String password) {
        Optional<UserAccount> user = findByLogin(login.trim());
        String hash = user.map(UserAccount::getPasswordHash).orElse(dummyPasswordHash);
        if (!passwordEncoder.matches(password, hash) || user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nieprawidłowy login lub hasło.");
        }
        return startSession(user.get());
    }

    /** Nowa sesja dla konta, np. zaraz po rejestracji. */
    @Transactional
    public AuthTokensResponse startSession(UserAccount user) {
        String refreshToken = newRefreshToken();
        Instant now = clock.instant();
        AuthSession session = sessions.save(new AuthSession(user.getId(), sha256(refreshToken), now,
                now.plus(properties.refreshTokenTtl())));
        return tokens(user, session, refreshToken);
    }

    /** Bez wycofania przy 401: unieważnienie sesji po wykryciu ponownego użycia musi zostać zapisane. */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public AuthTokensResponse refresh(String refreshToken) {
        Instant now = clock.instant();
        String hash = sha256(refreshToken);
        // Blokada wiersza sesji: równoległe odświeżenia tym samym tokenem (np. dwie karty) idą po kolei.
        // Drugie widzi już zrotowany skrót i trafia do gałęzi poniżej.
        AuthSession session = sessions.findForRefreshByTokenHash(hash).orElse(null);
        if (session == null) {
            usedTokens.findById(hash).ifPresent(used -> handleReuse(used, now));
            throw sessionExpired();
        }
        if (!session.isActive(now)) {
            throw sessionExpired();
        }
        usedTokens.save(new UsedRefreshToken(hash, session.getId(), now));
        String rotated = newRefreshToken();
        session.rotate(sha256(rotated), now.plus(properties.refreshTokenTtl()));
        // Role w tokenie odświeżają się tutaj: po zmianie ról (onboarding) frontend dostaje je przy najbliższym odświeżeniu.
        return tokens(accounts.get(session.getUserId()), session, rotated);
    }

    /**
     * Zrotowany token użyty ponownie tuż po rotacji to zwykle druga karta tej samej przeglądarki: dostaje 401
     * (frontend przejmuje wtedy tokeny zapisane przez pierwszą kartę), a sesja trwa dalej. Później to sygnał kradzieży.
     */
    private void handleReuse(UsedRefreshToken used, Instant now) {
        if (now.isBefore(used.getUsedAt().plus(properties.refreshReuseGrace()))) {
            return;
        }
        sessions.findById(used.getSessionId()).ifPresent(reused -> {
            log.warn("Ponowne użycie zrotowanego refresh tokenu: sesja {} użytkownika {} unieważniona",
                    reused.getId(), reused.getUserId());
            reused.revoke(now);
        });
    }

    /** Wylogowanie z bieżącej sesji ({@code sid} z access tokenu); inne urządzenia zostają zalogowane. */
    @Transactional
    public void logout(UUID sessionId) {
        sessions.findById(sessionId).ifPresent(session -> session.revoke(clock.instant()));
    }

    private Optional<UserAccount> findByLogin(String login) {
        if (Contacts.looksLikeEmail(login)) {
            return users.findByEmail(Contacts.email(login));
        }
        try {
            return users.findByPhone(Contacts.phone(login));
        } catch (ResponseStatusException notAPhoneNumber) {
            return Optional.empty();
        }
    }

    private AuthTokensResponse tokens(UserAccount user, AuthSession session, String refreshToken) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .claim("roles", user.getRoles().stream().sorted().map(Enum::name).toList())
                .claim("sid", session.getId().toString())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthTokensResponse(accessToken, refreshToken, properties.accessTokenTtl().toSeconds());
    }

    private static ResponseStatusException sessionExpired() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesja wygasła. Zaloguj się ponownie.");
    }

    private static String newRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
