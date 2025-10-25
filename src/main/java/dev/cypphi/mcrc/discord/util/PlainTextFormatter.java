package dev.cypphi.mcrc.discord.util;

import dev.cypphi.mcrc.discord.util.chat.ChatFormattingUtil;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class PlainTextFormatter implements IDiscordMessageFormatter {
    @Override
    public MessageCreateData format(DiscordMessageSpec spec) {
        StringBuilder builder = new StringBuilder();
        if (spec.title() != null && !spec.title().isBlank()) {
            builder.append("**").append(spec.title().trim()).append("**").append("\n");
        }
        builder.append(spec.description());

        if (!spec.fields().isEmpty()) {
            builder.append("\n");
            for (DiscordMessageSpec.Field field : spec.fields()) {
                builder.append("\n").append(field.name()).append(": ").append(field.value());
            }
        }

        if (spec.footer() != null && !spec.footer().isBlank()) {
            builder.append("\n\n_").append(spec.footer().trim()).append("_");
        }

        if (spec.overrideColor() != null) {
            builder.append("\nColor: ").append(ChatFormattingUtil.toHex(spec.overrideColor()));
        }

        if (spec.imageUrl() != null && !spec.imageUrl().isBlank()) {
            builder.append("\n\n[Image attached]");
        }

        return MessageCreateData.fromContent(builder.toString());
    }
}
