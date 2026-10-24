package pl.rynek.identity.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.rynek.identity.api.dto.ConfirmPhoneVerificationRequest;
import pl.rynek.identity.api.dto.LoginRequest;
import pl.rynek.identity.api.dto.LoginWithGoogleRequest;
import pl.rynek.identity.api.dto.RefreshTokenRequest;
import pl.rynek.identity.api.dto.RegisterRequest;
import pl.rynek.identity.api.dto.RequestPasswordResetRequest;
import pl.rynek.identity.api.dto.RequestPhoneVerificationRequest;
import pl.rynek.identity.api.dto.ResetPasswordRequest;

import static pl.rynek.identity.common.NotImplemented.notImplemented;

@RestController
public class AuthController {

    /**
     * Rejestracja (e-mail/telefon + hasło). Dostęp: pub.
     */
    @PostMapping("/api/v1/auth/register")
    public void register(@RequestBody RegisterRequest request) {
        throw notImplemented();
    }

    /**
     * Logowanie, zwraca access + refresh token. Dostęp: pub.
     */
    @PostMapping("/api/v1/auth/login")
    public void login(@RequestBody LoginRequest request) {
        throw notImplemented();
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
    public void refreshToken(@RequestBody RefreshTokenRequest request) {
        throw notImplemented();
    }

    /**
     * Unieważnienie refresh tokenu. Dostęp: user.
     */
    @PostMapping("/api/v1/auth/logout")
    public void logout() {
        throw notImplemented();
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
