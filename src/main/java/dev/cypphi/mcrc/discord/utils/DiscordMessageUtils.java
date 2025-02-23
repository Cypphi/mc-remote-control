package dev.cypphi.mcrc.discord.utils;

import dev.cypphi.mcrc.MinecraftRemoteControl;
import dev.cypphi.mcrc.config.Config;

import java.util.Objects;

public class DiscordMessageUtils {

    public static void sendToDiscord(String message) {
        Objects.requireNonNull(MinecraftRemoteControl.getJDA().getTextChannelById(Config.getInstance().getDiscordChannelID())).sendMessage(message).queue();
    }
}
