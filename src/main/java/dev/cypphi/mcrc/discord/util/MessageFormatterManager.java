package dev.cypphi.mcrc.discord.util;

import dev.cypphi.mcrc.config.MCRCConfig;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class MessageFormatterManager {

    public static MessageCreateData format(String content, String type) {
        boolean useEmbeds = MCRCConfig.HANDLER.instance().useEmbeds;

        IDiscordMessageFormatter formatter = useEmbeds
                ? new EmbedFormatter()
                : new PlainTextFormatter();

        return formatter.format(content, type);
    }
}
