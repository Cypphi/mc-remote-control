package dev.cypphi.mcrc.discord.util;

import dev.cypphi.mcrc.config.MCRCConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.time.Instant;

public class EmbedFormatter implements IDiscordMessageFormatter {
    @Override
    public MessageCreateData format(DiscordMessageSpec spec) {
        EmbedBuilder builder = new EmbedBuilder();

        if (spec.title() != null && !spec.title().isBlank()) {
            builder.setTitle(spec.title());
        }

        builder.setDescription(spec.description());

        boolean useColors = MCRCConfig.HANDLER.instance().useEmbedColors;
        if (useColors && spec.kind() != null) {
            builder.setColor(spec.kind().color());
        }

        for (DiscordMessageSpec.Field field : spec.fields()) {
            builder.addField(field.name(), field.value(), field.inline());
        }

        if (spec.footer() != null && !spec.footer().isBlank()) {
            builder.setFooter(spec.footer());
        }

        if (spec.thumbnailUrl() != null && !spec.thumbnailUrl().isBlank()) {
            builder.setThumbnail(spec.thumbnailUrl());
        }

        if (spec.imageUrl() != null && !spec.imageUrl().isBlank()) {
            builder.setImage(spec.imageUrl());
        }

        if (spec.timestamp()) {
            builder.setTimestamp(Instant.now());
        }

        return MessageCreateData.fromEmbeds(builder.build());
    }
}
