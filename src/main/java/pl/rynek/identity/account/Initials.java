package pl.rynek.identity.account;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Inicjały do awatara, tak jak {@code initials()} we frontendzie: „Dom i Kawa” → „DK”, „Anna” → „AN”. */
public final class Initials {

    private Initials() {
    }

    public static String of(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        // Spójniki („i”, „w”, „z”) pomijamy; jednoliterowe wielkie litery zostają.
        List<String> parts = Arrays.stream(name.replaceAll("[^\\p{L}\\s-]", " ").split("[\\s-]+"))
                .filter(p -> !p.isEmpty())
                .filter(p -> p.length() > 1 || p.equals(p.toUpperCase(Locale.ROOT)))
                .toList();
        String initials = parts.size() >= 2
                ? "" + parts.get(0).charAt(0) + parts.get(1).charAt(0)
                : name.substring(0, Math.min(2, name.length()));
        return initials.toUpperCase(Locale.ROOT);
    }
}
