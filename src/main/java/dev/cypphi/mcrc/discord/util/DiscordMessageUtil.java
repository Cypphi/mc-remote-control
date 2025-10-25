package dev.cypphi.mcrc.discord.util;

import dev.cypphi.mcrc.MinecraftRemoteControl;
import dev.cypphi.mcrc.config.MCRCConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

public class DiscordMessageUtil {
    public static void sendMessage(String message, String type) {
        sendMessage(DiscordMessageSpec.builder()
                .description(message)
                .kind(DiscordMessageKind.of(type))
                .build());
    }

    public static void sendMessage(DiscordMessageSpec spec) {
        JDA jda = MinecraftRemoteControl.getJDA();
        if (jda == null) {
            MinecraftRemoteControl.LOGGER.warn("Cannot send message: Discord bot is not connected.");
            return;
        }

        String channelID = MCRCConfig.HANDLER.instance().discordChannel;
        if (channelID == null || channelID.isBlank()) {
            MinecraftRemoteControl.LOGGER.warn("Cannot send message: Discord channel ID is not configured.");
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelID.trim());

        if (channel != null) {
            MessageCreateData payload = MessageFormatterManager.format(spec);
            channel.sendMessage(payload).queue();
        } else {
            MinecraftRemoteControl.LOGGER.error("Failed to send message: Discord channel not found (ID: {})", channelID);
        }
    }

    public static void editMessage(String messageId, String newContent, String type) {
        editMessage(messageId, DiscordMessageSpec.builder()
                .description(newContent)
                .kind(DiscordMessageKind.of(type))
                .build());
    }

    public static void editMessage(String messageId, DiscordMessageSpec spec) {
        JDA jda = MinecraftRemoteControl.getJDA();
        if (jda == null) {
            MinecraftRemoteControl.LOGGER.warn("Cannot edit message: Discord bot is not connected.");
            return;
        }

        String channelID = MCRCConfig.HANDLER.instance().discordChannel;
        if (channelID == null || channelID.isBlank()) {
            MinecraftRemoteControl.LOGGER.warn("Cannot edit message: Discord channel ID is not configured.");
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelID.trim());

        if (channel != null) {
            channel.retrieveMessageById(messageId).queue(
                    message -> {
                        if (!message.getAuthor().isBot() ||
                                !message.getAuthor().getId().equals(channel.getJDA().getSelfUser().getId())) {
                            MinecraftRemoteControl.LOGGER.warn("Tried to edit message not sent by bot (ID: {})", messageId);
                            return;
                        }

                        MessageCreateData formatted = MessageFormatterManager.format(spec);
                        MessageEditData editData = MessageEditData.fromCreateData(formatted);

                        message.editMessage(editData).queue();
                    },
                    e -> MinecraftRemoteControl.LOGGER.error("Failed to retrieve message (ID: {}): {}", messageId, e.getMessage())
            );
        } else {
            MinecraftRemoteControl.LOGGER.error("Failed to edit message: Discord channel not found (ID: {})", channelID);
        }
    }
}
