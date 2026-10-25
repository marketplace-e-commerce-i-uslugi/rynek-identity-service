package pl.rynek.identity.account;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.rynek.identity.api.dto.RegisterRequest;
import pl.rynek.identity.api.dto.UpdateMeRequest;

@Service
public class AccountService {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AccountService(UserAccountRepository users, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public UserAccount register(RegisterRequest request) {
        String email = isBlank(request.email()) ? null : Contacts.email(request.email());
        String phone = isBlank(request.phone()) ? null : Contacts.phone(request.phone());
        if (email == null && phone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Podaj e-mail albo numer telefonu.");
        }
        if (email != null && users.existsByEmail(email)) {
            throw emailTaken();
        }
        if (phone != null && users.existsByPhone(phone)) {
            throw phoneTaken();
        }
        UserAccount user = new UserAccount(email, phone, passwordEncoder.encode(request.password()),
                request.displayName().trim(), clock.instant());
        users.save(user);
        flushOrConflict();
        return user;
    }

    @Transactional(readOnly = true)
    public UserAccount get(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleźliśmy tego konta."));
    }

    @Transactional
    public UserAccount update(UUID userId, UpdateMeRequest request) {
        UserAccount user = get(userId);
        if (request.displayName() != null) {
            user.changeDisplayName(request.displayName().trim(), clock.instant());
        }
        if (!isBlank(request.email())) {
            String email = Contacts.email(request.email());
            if (!email.equals(user.getEmail()) && users.existsByEmail(email)) {
                throw emailTaken();
            }
            user.changeEmail(email, clock.instant());
        }
        if (!isBlank(request.phone())) {
            String phone = Contacts.phone(request.phone());
            if (!phone.equals(user.getPhone()) && users.existsByPhone(phone)) {
                throw phoneTaken();
            }
            user.changePhone(phone, clock.instant());
        }
        flushOrConflict();
        return user;
    }

    @Transactional
    public UserAccount chooseRoles(UUID userId, Set<Role> roles) {
        if (roles.contains(Role.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nie możesz nadać sobie roli administratora.");
        }
        UserAccount user = get(userId);
        user.chooseRoles(roles, clock.instant());
        return user;
    }

    /**
     * Kanał utworzony w channel-service (tymczasowo zamiast zdarzenia {@code channel.created}). Rolę sprawdza się tu,
     * a nie w channel-service, bo access token tuż po onboardingu ma jeszcze stare role. Ponowne przypięcie tego samego
     * kanału niczego nie zmienia.
     */
    @Transactional
    public UserAccount linkChannel(UUID userId, UUID channelId, String channelHandle) {
        UserAccount user = get(userId);
        if (!user.isSeller()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Kanał może założyć usługodawca albo merchant. Wybierz rolę w ustawieniach konta.");
        }
        if (user.getChannelId() != null && !user.getChannelId().equals(channelId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Masz już kanał. Jedno konto może mieć jeden kanał.");
        }
        user.linkChannel(channelId, channelHandle, clock.instant());
        return user;
    }

    /**
     * Dwa równoległe żądania na ten sam login mogą oba przejść sprawdzenie {@code exists...}; wtedy rozstrzyga
     * unikalny indeks w bazie. Zapis od razu, żeby taki konflikt dał 409, a nie błąd przy zatwierdzaniu transakcji.
     */
    private void flushOrConflict() {
        try {
            users.flush();
        } catch (DataIntegrityViolationException e) {
            String message = String.valueOf(e.getMostSpecificCause().getMessage());
            throw message.contains("users_phone_key") ? phoneTaken() : emailTaken();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static ResponseStatusException emailTaken() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Konto z tym adresem e-mail już istnieje.");
    }

    private static ResponseStatusException phoneTaken() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Konto z tym numerem telefonu już istnieje.");
    }
}
