package dev.cypphi.mcrc.discord.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public interface SlashCommand {
    String getName();

    String getDescription();

    default CommandData asCommandData() {
        return Commands.slash(getName(), getDescription());
    }

    void execute(SlashCommandInteractionEvent event);
}
