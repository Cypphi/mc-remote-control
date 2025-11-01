package dev.cypphi.mcrc.util.chat;

import com.mojang.authlib.GameProfile;
import dev.cypphi.mcrc.util.minecraft.MojangProfileResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

public final class ChatSenderResolver {
    private static final Method PROFILE_NAME_ACCESSOR = resolveProfileNameAccessor();

    private ChatSenderResolver() {}

    public static ChatSenderDetails resolve(Text originalMessage,
                                            UUID trackedSender,
                                            MessageIndicator indicator,
                                            String rawContent,
                                            boolean serverMessage,
                                            boolean localMessage) {
        MinecraftClient client = MinecraftClient.getInstance();

        ChatSenderDetails tracked = resolveTrackedSender(client, trackedSender);
        if (tracked != null) {
            return tracked;
        }

        UUID cached = PlayerMessageTracker.consume(originalMessage);
        if (cached != null) {
            String name = resolveNameFromUuid(client, cached);
            String avatar = buildAvatarUrl(cached);
            return new ChatSenderDetails(name, cached, avatar);
        }

        String leadingName = ChatNameUtil.extractLeadingName(rawContent);
        if (leadingName != null) {
            ChatSenderDetails fromName = resolveByName(client, leadingName);
            if (fromName != null) {
                return fromName;
            }
        }

        String bracketedName = ChatNameUtil.extractBracketedName(rawContent);
        if (bracketedName != null && bracketedName.isBlank()) {
            bracketedName = null;
        }

        if (serverMessage || indicator != null) {
            if (bracketedName != null) {
                return new ChatSenderDetails(bracketedName, null, buildAvatarUrl(bracketedName));
            }
            return senderFromIndicator(indicator, serverMessage);
        }

        if (bracketedName != null) {
            ChatSenderDetails bracketed = resolveByName(client, bracketedName);
            if (bracketed != null) {
                return bracketed;
            }

            MojangProfileResolver.Profile profile = MojangProfileResolver.getCachedProfile(bracketedName);
            if (profile != null && profile.uuid() != null) {
                UUID uuid = profile.uuid();
                return new ChatSenderDetails(profile.name(), uuid, buildAvatarUrl(uuid));
            }
            MojangProfileResolver.queueLookup(bracketedName);

            return new ChatSenderDetails(bracketedName, null, buildAvatarUrl(bracketedName));
        }

        if (localMessage) {
            return new ChatSenderDetails("client", null, null);
        }

        return new ChatSenderDetails("server", null, null);
    }

    private static ChatSenderDetails resolveTrackedSender(MinecraftClient client, UUID senderUuid) {
        if (client == null || senderUuid == null) {
            return null;
        }

        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) {
            return null;
        }

        PlayerListEntry entry = handler.getPlayerListEntry(senderUuid);
        if (entry == null) {
            return null;
        }

        Text displayName = entry.getDisplayName();
        if (displayName != null) {
            String pretty = displayName.getString();
            if (!pretty.isBlank()) {
                return new ChatSenderDetails(pretty, senderUuid, buildAvatarUrl(senderUuid));
            }
        }

        GameProfile profile = entry.getProfile();
        String profileName = readProfileName(profile);
        if (profileName != null && !profileName.isBlank()) {
            return new ChatSenderDetails(profileName, senderUuid, buildAvatarUrl(senderUuid));
        }

        return new ChatSenderDetails("server", null, null);
    }

    private static String resolveNameFromUuid(MinecraftClient client, UUID uuid) {
        if (client != null) {
            ClientPlayNetworkHandler handler = client.getNetworkHandler();
            if (handler != null) {
                PlayerListEntry entry = handler.getPlayerListEntry(uuid);
                if (entry != null) {
                    GameProfile profile = entry.getProfile();
                    String name = profile != null ? readProfileName(profile) : null;
                    if (name != null && !name.isBlank()) {
                        return name;
                    }
                }
            }
        }
        return uuid.toString();
    }

    private static ChatSenderDetails resolveByName(MinecraftClient client, String name) {
        if (client == null || name == null || name.isBlank()) {
            return null;
        }

        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) {
            return null;
        }

        PlayerListEntry entry = handler.getPlayerListEntry(name);
        if (entry == null) {
            return null;
        }

        GameProfile profile = entry.getProfile();
        if (profile == null || profile.getId() == null) {
            return null;
        }

        UUID uuid = profile.getId();
        String resolvedName = profile.getName() != null ? profile.getName() : name;
        return new ChatSenderDetails(resolvedName, uuid, buildAvatarUrl(uuid));
    }

    private static ChatSenderDetails senderFromIndicator(MessageIndicator indicator, boolean serverMessage) {
        if (indicator != null && indicator.icon() != null) {
            String title = formatIndicatorName(indicator.icon());
            return new ChatSenderDetails(title, null, null);
        }
        return serverMessage ? new ChatSenderDetails("server", null, null) : new ChatSenderDetails("client", null, null);
    }

    private static String formatIndicatorName(MessageIndicator.Icon icon) {
        String raw = icon.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = raw.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
            builder.append(' ');
        }
        return builder.isEmpty() ? "Server" : builder.toString().strip();
    }

    private static Method resolveProfileNameAccessor() {
        try {
            return GameProfile.class.getMethod("getName");
        } catch (NoSuchMethodException ignored) {
            try {
                return GameProfile.class.getMethod("name");
            } catch (NoSuchMethodException innerIgnored) {
                return null;
            }
        }
    }

    private static String readProfileName(GameProfile profile) {
        if (profile == null || PROFILE_NAME_ACCESSOR == null) {
            return null;
        }

        try {
            Object value = PROFILE_NAME_ACCESSOR.invoke(profile);
            if (value instanceof String str) {
                return str;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    public static String buildAvatarUrl(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        String sanitizedUuid = uuid.toString().replace("-", "");
        return "https://api.mineatar.io/head/" + sanitizedUuid + "?scale=8";
    }

    public static String buildAvatarUrl(String username) {
        if (username == null) {
            return null;
        }

        String sanitized = username.strip();
        if (sanitized.isEmpty()) {
            return null;
        }

        if ("server".equalsIgnoreCase(sanitized) || "client".equalsIgnoreCase(sanitized)) {
            return null;
        }

        for (int i = 0; i < sanitized.length(); i++) {
            if (!ChatNameUtil.isValidNameChar(sanitized.charAt(i))) {
                return null;
            }
        }

        return "https://minotar.net/helm/" + sanitized + "/64";
    }
}
