package dev.cypphi.mcrc.discord.util;

import dev.cypphi.mcrc.MinecraftRemoteControl;
import dev.cypphi.mcrc.config.MCRCConfig;

public class DiscordMessageUtil {
    public static void sendMessage(String message) {
        MinecraftRemoteControl.getJDA().getTextChannelById(MCRCConfig.HANDLER.instance().discordChannel).sendMessage(message).queue();
    }
}
