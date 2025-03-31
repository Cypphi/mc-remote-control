package dev.cypphi.mcrc.discord.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.*;

public class EmbedFormatter implements IDiscordMessageFormatter {
    @Override
    public MessageCreateData format(String content, String type) {
        return MessageCreateData.fromEmbeds(
                new EmbedBuilder()
                        .setDescription(content)
                        .setColor(getColor(type))
                        .build()
        );
    }

    private static Color getColor(String type) {
        return switch (type) {
            case "ready" -> Color.GREEN;
            case "info" -> Color.ORANGE;
            case "warning" -> Color.RED;
            default -> null;
        };
    }
}
