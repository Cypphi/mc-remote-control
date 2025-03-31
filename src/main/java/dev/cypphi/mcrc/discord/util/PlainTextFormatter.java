package dev.cypphi.mcrc.discord.util;

import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class PlainTextFormatter implements IDiscordMessageFormatter {
    @Override
    public MessageCreateData format(String content, String type) {
        return MessageCreateData.fromContent(content);
    }
}
