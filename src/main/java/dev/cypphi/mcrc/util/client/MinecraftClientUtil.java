package dev.cypphi.mcrc.util.client;

import net.minecraft.client.MinecraftClient;

import java.util.Optional;

public final class MinecraftClientUtil {
    private MinecraftClientUtil() {}

    public static Optional<String> getLocalUsername() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getSession() == null) {
            return Optional.empty();
        }

        String username = client.getSession().getUsername();
        if (username == null) {
            return Optional.empty();
        }

        String trimmed = username.trim();
        return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
    }
}
