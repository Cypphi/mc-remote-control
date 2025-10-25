package dev.cypphi.mcrc.discord.command.commands;

import dev.cypphi.mcrc.discord.command.SlashCommand;
import dev.cypphi.mcrc.config.MCRCConfig;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class RemoteViewCommand implements SlashCommand {
    @Override
    public String getName() {
        return "remoteview";
    }

    @Override
    public String getDescription() {
        return "Generate a secure Remote View link for this client.";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!MCRCConfig.HANDLER.instance().remoteViewEnabled) {
            event.reply("Remote View is disabled in the client settings. Enable it in-game to use this command.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.reply("Remote View setup is not available yet, please try again later.")
                .setEphemeral(true)
                .queue();
    }
}
