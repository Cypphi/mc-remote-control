package dev.cypphi.mcrc.discord.util.chat;

import com.mojang.authlib.GameProfile;
import dev.cypphi.mcrc.discord.util.DiscordMessageSpec;
import dev.cypphi.mcrc.discord.util.DiscordMessageUtil;
import dev.cypphi.mcrc.discord.util.MojangProfileResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

public final class ChatLogUtil {
    private static final Method PROFILE_NAME_ACCESSOR = resolveProfileNameAccessor();

    private ChatLogUtil() {}

    public static void logOutgoing(String rawContent, boolean command) {
        ChatFormattingUtil.ChatAnalysis analysis = ChatFormattingUtil.analyze(rawContent);

        DiscordMessageSpec spec = DiscordMessageSpec.builder()
                .title(command ? "Command Sent" : "Chat Message Sent")
                .description(codeBlock(analysis.content()))
                .colorOverride(analysis.color())
                .timestamp(true)
                .build();

        DiscordMessageUtil.sendMessage(spec);
    }

    public static void logIncoming(Text message, MessageIndicator indicator, UUID sender, boolean serverMessage, boolean localMessage) {
        ChatFormattingUtil.ChatAnalysis analysis = ChatFormattingUtil.analyze(message);
        String channel = resolveChannel(indicator);

        SenderDetails details = resolveSenderDetails(message, sender, indicator, analysis.content(), serverMessage, localMessage);

        DiscordMessageSpec.Builder builder = DiscordMessageSpec.builder()
                .title(formatChannelLabel(channel))
                .description(buildDescription(details, analysis.content()))
                .colorOverride(analysis.color())
                .timestamp(true);

        if (details.avatarUrl() != null) {
            builder.thumbnailUrl(details.avatarUrl());
        }

        DiscordMessageUtil.sendMessage(builder.build());
    }

    private static String codeBlock(String content) {
        return "```" + (content == null ? "" : content) + "```";
    }

    private static String resolveChannel(MessageIndicator indicator) {
        if (indicator == null) {
            return "chat";
        }

        MessageIndicator.Icon icon = indicator.icon();
        if (icon == null) {
            return "chat";
        }

        return icon.name().toLowerCase(Locale.ROOT);
    }

    private static String formatChannelLabel(String channel) {
        if (channel == null || channel.isBlank()) {
            return "In-game Chat";
        }

        if ("chat".equalsIgnoreCase(channel.trim())) {
            return "In-game Chat";
        }

        String normalized = channel.replace('_', ' ').trim();
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            String capitalized = part.substring(0, 1).toUpperCase(Locale.ROOT) +
                    part.substring(1).toLowerCase(Locale.ROOT);
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(capitalized);
        }

        if (builder.isEmpty()) {
            return "In-game Chat";
        }

        if (!builder.toString().toLowerCase(Locale.ROOT).contains("chat")) {
            builder.append(" Chat");
        }

        return builder.toString();
    }

    private record SenderDetails(String name, UUID uuid, String avatarUrl) {}

    private static SenderDetails resolveSenderDetails(Text originalMessage, UUID trackedSender, MessageIndicator indicator, String rawContent, boolean serverMessage, boolean localMessage) {
        MinecraftClient client = MinecraftClient.getInstance();

        SenderDetails tracked = resolveTrackedSender(client, trackedSender);
        if (tracked != null) {
            return tracked;
        }

        UUID cached = PlayerMessageTracker.consume(originalMessage);
        if (cached != null) {
            String name = resolveNameFromUuid(client, cached);
            String avatar = buildAvatarUrl(cached);
            return new SenderDetails(name, cached, avatar);
        }

        String leadingName = extractLeadingName(rawContent);
        if (leadingName != null) {
            SenderDetails fromName = resolveByName(client, leadingName);
            if (fromName != null) {
                return fromName;
            }
        }

        if (serverMessage || indicator != null) {
            return senderFromIndicator(indicator);
        }

        if (localMessage) {
            return new SenderDetails("client", null, null);
        }

        ParsedName parsed = parseBracketedName(rawContent);
        if (parsed != null) {
            MojangProfileResolver.Profile profile = MojangProfileResolver.getCachedProfile(parsed.name());
            if (profile != null && profile.uuid() != null) {
                UUID uuid = profile.uuid();
                return new SenderDetails(profile.name(), uuid, buildAvatarUrl(uuid));
            }
            MojangProfileResolver.queueLookup(parsed.name());
        }

        if (serverMessage) {
            return new SenderDetails("server", null, null);
        }

        return new SenderDetails("client", null, null);
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

    private static SenderDetails resolveByName(MinecraftClient client, String name) {
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
        String avatar = buildAvatarUrl(uuid);
        return new SenderDetails(resolvedName, uuid, avatar);
    }

    private static SenderDetails resolveTrackedSender(MinecraftClient client, UUID senderUuid) {
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
                return new SenderDetails(pretty, senderUuid, buildAvatarUrl(senderUuid));
            }
        }

        GameProfile profile = entry.getProfile();
        String profileName = readProfileName(profile);
        if (profileName != null && !profileName.isBlank()) {
            return new SenderDetails(profileName, senderUuid, buildAvatarUrl(senderUuid));
        }

        return new SenderDetails("server", null, null);
    }

    private static String extractLeadingName(String content) {
        if (content == null) {
            return null;
        }

        String remaining = content.stripLeading();

        boolean trimmed = true;
        while (trimmed) {
            trimmed = false;
            if (remaining.startsWith("[")) {
                int end = remaining.indexOf(']');
                if (end > 0) {
                    remaining = remaining.substring(end + 1).stripLeading();
                    trimmed = true;
                    continue;
                }
            }
            if (remaining.startsWith("(")) {
                int end = remaining.indexOf(')');
                if (end > 0) {
                    remaining = remaining.substring(end + 1).stripLeading();
                    trimmed = true;
                }
            }
        }

        if (remaining.isEmpty()) {
            return null;
        }

        int i = 0;
        int len = remaining.length();
        while (i < len && isValidNameChar(remaining.charAt(i))) {
            i++;
        }

        if (i == 0 || i > 16) {
            return null;
        }

        return remaining.substring(0, i);
    }

    private static boolean isValidNameChar(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    private record ParsedName(String name) {}

    private static ParsedName parseBracketedName(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return null;
        }

        String trimmed = rawContent.stripLeading();
        if (!trimmed.startsWith("<")) {
            return null;
        }

        int closing = trimmed.indexOf('>');
        if (closing <= 1) {
            return null;
        }

        String candidate = trimmed.substring(1, closing).trim();
        if (candidate.isEmpty() || candidate.length() > 16) {
            return null;
        }

        for (char c : candidate.toCharArray()) {
            if (!(c == '_' || Character.isLetterOrDigit(c))) {
                return null;
            }
        }

        return new ParsedName(candidate);
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

    private static SenderDetails senderFromIndicator(MessageIndicator indicator) {
        if (indicator != null && indicator.icon() != null) {
            String title = formatIndicatorName(indicator.icon());
            return new SenderDetails(title, null, null);
        }
        return new SenderDetails("server", null, null);
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

    private static String buildAvatarUrl(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return "https://mc-heads.net/avatar/" + uuid + "/128.png";
    }

    private static String buildDescription(SenderDetails details, String content) {
        StringBuilder sb = new StringBuilder();
        boolean includeSender = details.uuid() == null || details.avatarUrl() == null;
        if (includeSender && details.name() != null && !details.name().isBlank()) {
            sb.append("**Sender:** ").append(details.name().strip());
        }
        if (content != null && !content.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(codeBlock(content));
        }
        return sb.isEmpty() ? " " : sb.toString();
    }
}
