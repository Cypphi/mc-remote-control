package dev.cypphi.mcrc.discord.util;

import dev.cypphi.mcrc.MinecraftRemoteControl;
import dev.cypphi.mcrc.config.MCRCConfig;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class DiscordMessageUtil {
    public static void sendMessage(String message, String type) {
        String channelID = MCRCConfig.HANDLER.instance().discordChannel;
        TextChannel channel = MinecraftRemoteControl.getJDA().getTextChannelById(channelID);

        if (channel != null) {
            channel.sendMessage(MessageFormatterManager.format(message, type)).queue();
        } else {
            MinecraftRemoteControl.LOGGER.error("Failed to send message: Discord channel not found (ID: {})", channelID);
        }
    }

    public static void editMessage(String messageId, String newContent, String type) {
        String channelID = MCRCConfig.HANDLER.instance().discordChannel;
        TextChannel channel = MinecraftRemoteControl.getJDA().getTextChannelById(channelID);

        if (channel != null) {
            channel.retrieveMessageById(messageId).queue(
                    message -> {
                        if (!message.getAuthor().isBot() ||
                                !message.getAuthor().getId().equals(channel.getJDA().getSelfUser().getId())) {
                            MinecraftRemoteControl.LOGGER.warn("Tried to edit message not sent by bot (ID: {})", messageId);
                            return;
                        }

                        MessageCreateData formatted = MessageFormatterManager.format(newContent, type);

                        if (formatted.getEmbeds().isEmpty()) {
                            message.editMessage(formatted.getContent()).queue();
                        } else {
                            message.editMessageEmbeds(formatted.getEmbeds()).queue();
                        }
                    },
                    e -> MinecraftRemoteControl.LOGGER.error("Failed to retrieve message (ID: {}): {}", messageId, e.getMessage())
            );
        } else {
            MinecraftRemoteControl.LOGGER.error("Failed to edit message: Discord channel not found (ID: {})", channelID);
        }
    }
}
