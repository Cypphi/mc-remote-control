package dev.cypphi.mcrc.discord.util;

import dev.cypphi.mcrc.MinecraftRemoteControl;
import dev.cypphi.mcrc.config.MCRCConfig;

import java.util.Objects;

public class DiscordMessageUtil {
    public static void sendMessage(String message, String type) {
        Objects.requireNonNull(MinecraftRemoteControl.getJDA().getTextChannelById(MCRCConfig.HANDLER.instance().discordChannel)).sendMessage(MessageFormatterManager.format(message, type)).queue();
    }
}
