package dev.cypphi.mcrc.discord.command;

import dev.cypphi.mcrc.config.MCRCConfig;
import dev.cypphi.mcrc.util.discord.DiscordPingUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class SlashCommandListener extends ListenerAdapter {
    private final CommandRegistry registry;

    public SlashCommandListener(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        MCRCConfig config = MCRCConfig.HANDLER.instance();

        String configuredChannelId = config.discordChannel == null ? "" : config.discordChannel.trim();
        if (!config.allowCommandsAnywhere && !configuredChannelId.isEmpty() && !configuredChannelId.equals(event.getChannel().getId())) {
            event.reply("Commands are only allowed in <#" + configuredChannelId + ">.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!config.allowPublicCommands) {
            String allowedUserId = DiscordPingUtil.allowedUserId();
            if (allowedUserId == null) {
                event.reply("Command access is restricted but no Discord user ID has been configured. Please set one in the client options.")
                        .setEphemeral(true)
                        .queue();
                return;
            }

            if (!event.getUser().getId().equals(allowedUserId)) {
                event.reply("You are not authorized to control this client.")
                        .setEphemeral(true)
                        .queue();
                return;
            }
        }

        registry.handle(event);
    }
}
