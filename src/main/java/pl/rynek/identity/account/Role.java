package pl.rynek.identity.account;

/** Role konta (§1). Jedno konto może mieć kilka; {@code ADMIN} nadaje się poza aplikacją. */
public enum Role {
    BUYER,
    SERVICE_PROVIDER,
    MERCHANT,
    ADMIN
}
