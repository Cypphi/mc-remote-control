package dev.cypphi.mcrc.util.chat;

import dev.cypphi.mcrc.config.MCRCConfig;
import dev.cypphi.mcrc.util.discord.DiscordMessageSpec;
import dev.cypphi.mcrc.util.discord.DiscordMessageUtil;
import dev.cypphi.mcrc.util.discord.DiscordPingUtil;
import dev.cypphi.mcrc.util.client.MinecraftClientUtil;
import dev.cypphi.mcrc.util.chat.ChatNameUtil;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.UUID;

public final class ChatLogUtil {
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

        ChatSenderDetails details = ChatSenderResolver.resolve(message, sender, indicator, analysis.content(), serverMessage, localMessage);

        DiscordMessageSpec.Builder builder = DiscordMessageSpec.builder()
                .title(formatChannelLabel(channel))
                .description(buildDescription(details, analysis.content()))
                .colorOverride(analysis.color())
                .timestamp(true);

        if (details.avatarUrl() != null) {
            builder.thumbnailUrl(details.avatarUrl());
        }

        if (shouldPingOnMention(analysis.content(), localMessage, details)) {
            DiscordPingUtil.applyAllowedUserMention(builder, "Chat mention");
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

    private static String buildDescription(ChatSenderDetails details, String content) {
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

    private static boolean shouldPingOnMention(String content, boolean localMessage, ChatSenderDetails details) {
        if (localMessage || content == null || content.isBlank()) {
            return false;
        }

        MCRCConfig config = MCRCConfig.HANDLER.instance();
        if (config == null || !config.pingOnMention) {
            return false;
        }

        if (DiscordPingUtil.allowedUserId() == null) {
            return false;
        }

        return MinecraftClientUtil.getLocalUsername()
                .filter(username -> !isLocalSender(username, details))
                .filter(username -> ChatMentionUtil.containsUsernameMention(content, username))
                .isPresent();
    }

    private static boolean isLocalSender(String localUsername, ChatSenderDetails details) {
        if (localUsername == null || localUsername.isBlank()) {
            return false;
        }
        if (details == null || details.name() == null) {
            return false;
        }
        return localUsername.equalsIgnoreCase(details.name().strip());
    }
}
