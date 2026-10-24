package pl.rynek.identity.api;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.rynek.identity.account.AccountService;
import pl.rynek.identity.api.dto.AuthTokensResponse;
import pl.rynek.identity.api.dto.ConfirmPhoneVerificationRequest;
import pl.rynek.identity.api.dto.LoginRequest;
import pl.rynek.identity.api.dto.LoginWithGoogleRequest;
import pl.rynek.identity.api.dto.RefreshTokenRequest;
import pl.rynek.identity.api.dto.RegisterRequest;
import pl.rynek.identity.api.dto.RequestPasswordResetRequest;
import pl.rynek.identity.api.dto.RequestPhoneVerificationRequest;
import pl.rynek.identity.api.dto.ResetPasswordRequest;
import pl.rynek.identity.auth.AuthService;

import static pl.rynek.identity.common.NotImplemented.notImplemented;

@RestController
public class AuthController {

    private final AccountService accounts;
    private final AuthService auth;

    public AuthController(AccountService accounts, AuthService auth) {
        this.accounts = accounts;
        this.auth = auth;
    }

    /**
     * Rejestracja (e-mail/telefon + hasło). Dostęp: pub.
     */
    @PostMapping("/api/v1/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthTokensResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.startSession(accounts.register(request));
    }

    /**
     * Logowanie, zwraca access + refresh token. Dostęp: pub.
     */
    @PostMapping("/api/v1/auth/login")
    public AuthTokensResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request.login(), request.password());
    }

    /**
     * Logowanie przez Google. Dostęp: pub.
     */
    @PostMapping("/api/v1/auth/oauth/google")
    public void loginWithGoogle(@RequestBody LoginWithGoogleRequest request) {
        throw notImplemented();
    }

    /**
     * Odświeżenie tokenu. Dostęp: pub.
     */
    @PostMapping("/api/v1/auth/refresh")
    public AuthTokensResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return auth.refresh(request.refreshToken());
    }

    /**
     * Unieważnienie refresh tokenu. Dostęp: user.
     */
    @PostMapping("/api/v1/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Jwt token) {
        auth.logout(UUID.fromString(token.getClaimAsString("sid")));
    }

    /**
     * Prośba o reset hasła. Dostęp: pub.
     */
    @PostMapping("/api/v1/auth/password/forgot")
    public void requestPasswordReset(@RequestBody RequestPasswordResetRequest request) {
        throw notImplemented();
    }

    /**
     * Ustawienie nowego hasła. Dostęp: pub.
     */
    @PostMapping("/api/v1/auth/password/reset")
    public void resetPassword(@RequestBody ResetPasswordRequest request) {
        throw notImplemented();
    }

    /**
     * Wysłanie kodu SMS. Dostęp: user.
     */
    @PostMapping("/api/v1/auth/phone/verify/request")
    public void requestPhoneVerification(@RequestBody RequestPhoneVerificationRequest request) {
        throw notImplemented();
    }

    /**
     * Potwierdzenie kodu. Dostęp: user.
     */
    @PostMapping("/api/v1/auth/phone/verify/confirm")
    public void confirmPhoneVerification(@RequestBody ConfirmPhoneVerificationRequest request) {
        throw notImplemented();
    }
}
