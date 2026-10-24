package pl.rynek.identity.api.dto;

import java.util.List;

import pl.rynek.identity.account.Initials;
import pl.rynek.identity.account.Role;
import pl.rynek.identity.account.UserAccount;

/**
 * Profil użytkownika. Kontrakt: {@code User} w rynek-frontend/src/app/core/api/models/identity.ts.
 * {@code channelId}/{@code channelHandle} należą do channel-service; do czasu zdarzenia {@code channel.created} są puste.
 */
public record UserResponse(
        String id,
        String email,
        String phone,
        boolean phoneVerified,
        String displayName,
        String initials,
        String avatarUrl,
        List<Role> roles,
        String channelId,
        String channelHandle,
        boolean onboarded) {

    public static UserResponse of(UserAccount user) {
        return new UserResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getPhone(),
                user.isPhoneVerified(),
                user.getDisplayName(),
                Initials.of(user.getDisplayName()),
                user.getAvatarUrl(),
                user.getRoles().stream().sorted().toList(),
                null,
                null,
                user.isOnboarded());
    }
}
