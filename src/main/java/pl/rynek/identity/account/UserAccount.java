package pl.rynek.identity.account;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    private UUID id;

    private String email;

    private String phone;

    @Column(name = "phone_verified")
    private boolean phoneVerified;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    private boolean onboarded;

    /** Kanał z channel-service (usługodawca/merchant); pusty dla kupujących. */
    @Column(name = "channel_id")
    private UUID channelId;

    @Column(name = "channel_handle")
    private String channelHandle;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = EnumSet.noneOf(Role.class);

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected UserAccount() {
    }

    public UserAccount(String email, String phone, String passwordHash, String displayName, Instant now) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.roles = EnumSet.of(Role.BUYER);
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void changeDisplayName(String displayName, Instant now) {
        this.displayName = displayName;
        this.updatedAt = now;
    }

    public void changeEmail(String email, Instant now) {
        this.email = email;
        this.updatedAt = now;
    }

    /** Nowy numer trzeba potwierdzić od nowa (POST /auth/phone/verify/...). */
    public void changePhone(String phone, Instant now) {
        if (!phone.equals(this.phone)) {
            this.phone = phone;
            this.phoneVerified = false;
            this.updatedAt = now;
        }
    }

    /** Wybór ról z onboardingu kończy onboarding. */
    public void chooseRoles(Set<Role> roles, Instant now) {
        this.roles = EnumSet.copyOf(roles);
        this.onboarded = true;
        this.updatedAt = now;
    }

    public void linkChannel(UUID channelId, String channelHandle, Instant now) {
        this.channelId = channelId;
        this.channelHandle = channelHandle;
        this.updatedAt = now;
    }

    public boolean isSeller() {
        return roles.contains(Role.SERVICE_PROVIDER) || roles.contains(Role.MERCHANT);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public boolean isOnboarded() {
        return onboarded;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public String getChannelHandle() {
        return channelHandle;
    }

    public Set<Role> getRoles() {
        return roles;
    }
}
