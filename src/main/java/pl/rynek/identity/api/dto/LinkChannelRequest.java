package pl.rynek.identity.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Ciało żądania (int): kanał utworzony w channel-service, przypinany do konta.
 */
public record LinkChannelRequest(@NotNull UUID channelId, @NotBlank String channelHandle) {
}
