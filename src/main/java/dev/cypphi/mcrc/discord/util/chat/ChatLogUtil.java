package dev.cypphi.mcrc.discord.util.chat;

import dev.cypphi.mcrc.discord.util.DiscordMessageSpec;
import dev.cypphi.mcrc.discord.util.DiscordMessageUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
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

    public static void logIncoming(Text message, MessageIndicator indicator, UUID sender) {
        ChatFormattingUtil.ChatAnalysis analysis = ChatFormattingUtil.analyze(message);
        String channel = resolveChannel(indicator);

        String title = formatChannelLabel(channel);
        String senderName = resolveSenderName(sender);
        String footerText = "Sender: " + senderName;
        String avatarUrl = sender != null ? buildAvatarUrl(sender) : null;

        DiscordMessageSpec.Builder builder = DiscordMessageSpec.builder()
                .title(title)
                .description(codeBlock(analysis.content()))
                .colorOverride(analysis.color())
                .timestamp(true)
                .footer(footerText)
                .footerIconUrl(avatarUrl);

        if (sender != null) {
            builder.addField("Sender UUID", sender.toString(), true);
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

    private static String resolveSenderName(UUID sender) {
        if (sender == null) {
            return "client";
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            ClientPlayNetworkHandler handler = client.getNetworkHandler();
            if (handler != null) {
                PlayerListEntry entry = handler.getPlayerListEntry(sender);
                if (entry != null && entry.getProfile() != null && entry.getProfile().getName() != null) {
                    return entry.getProfile().getName();
                }
            }
        }

        return "client";
    }

    private static String buildAvatarUrl(UUID sender) {
        String uuid = sender.toString().replace("-", "");
        return "https://crafatar.com/avatars/" + uuid + "?size=32&overlay";
    }
}
