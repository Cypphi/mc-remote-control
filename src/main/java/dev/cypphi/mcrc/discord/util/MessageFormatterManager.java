package dev.cypphi.mcrc.discord.util;

import dev.cypphi.mcrc.config.MCRCConfig;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class MessageFormatterManager {

    public static MessageCreateData format(DiscordMessageSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("DiscordMessageSpec must not be null");
        }
        boolean useEmbeds = MCRCConfig.HANDLER.instance().useEmbeds;

        IDiscordMessageFormatter formatter = useEmbeds
                ? new EmbedFormatter()
                : new PlainTextFormatter();

        return formatter.format(spec);
    }
}
