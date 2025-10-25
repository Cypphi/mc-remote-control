package dev.cypphi.mcrc.discord.util.chat;

import dev.cypphi.mcrc.discord.util.DiscordMessageSpec;
import dev.cypphi.mcrc.discord.util.DiscordMessageUtil;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.Optional;
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

        String title = "Chat: " + channel;
        String thread = Thread.currentThread().getName();

        DiscordMessageSpec spec = DiscordMessageSpec.builder()
                .title(title)
                .description(codeBlock(analysis.content()))
                .colorOverride(analysis.color())
                .timestamp(true)
                .addField("Sender", Optional.ofNullable(sender).map(UUID::toString).orElse("unknown"), true)
                .build();

        DiscordMessageUtil.sendMessage(spec);
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
}
