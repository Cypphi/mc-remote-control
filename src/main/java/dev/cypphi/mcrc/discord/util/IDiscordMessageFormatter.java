package dev.cypphi.mcrc.discord.util;

import net.dv8tion.jda.api.utils.messages.MessageCreateData;

@FunctionalInterface
public interface IDiscordMessageFormatter {
    MessageCreateData format(DiscordMessageSpec spec);
}
