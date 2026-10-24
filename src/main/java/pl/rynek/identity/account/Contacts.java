package pl.rynek.identity.account;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Postać kanoniczna loginów: e-mail małymi literami, telefon w E.164 (9 cyfr bez prefiksu = numer polski). */
public final class Contacts {

    private static final Pattern PHONE_SEPARATORS = Pattern.compile("[\\s\\-()]");
    private static final Pattern POLISH_NUMBER = Pattern.compile("\\d{9}");
    private static final Pattern E164 = Pattern.compile("\\+[1-9]\\d{7,14}");

    private Contacts() {
    }

    public static String email(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static String phone(String raw) {
        String digits = PHONE_SEPARATORS.matcher(raw).replaceAll("");
        if (digits.startsWith("00")) {
            digits = "+" + digits.substring(2);
        }
        if (POLISH_NUMBER.matcher(digits).matches()) {
            return "+48" + digits;
        }
        if (E164.matcher(digits).matches()) {
            return digits;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieprawidłowy numer telefonu.");
    }

    public static boolean looksLikeEmail(String login) {
        return login.contains("@");
    }
}
