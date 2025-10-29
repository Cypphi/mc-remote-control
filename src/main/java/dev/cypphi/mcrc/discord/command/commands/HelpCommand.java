package dev.cypphi.mcrc.discord.command.commands;

import dev.cypphi.mcrc.MinecraftRemoteControl;
import dev.cypphi.mcrc.discord.bot.DiscordBot;
import dev.cypphi.mcrc.discord.command.CommandRegistry;
import dev.cypphi.mcrc.discord.command.SlashCommand;
import dev.cypphi.mcrc.util.discord.DiscordMessageKind;
import dev.cypphi.mcrc.util.discord.DiscordMessageSpec;
import dev.cypphi.mcrc.util.discord.MessageFormatterManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.Comparator;
import java.util.List;

public class HelpCommand implements SlashCommand {
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Show available commands and their description.";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        DiscordBot discordBot = MinecraftRemoteControl.getDiscordBot();
        if (discordBot == null) {
            event.reply("Command registry is not ready yet. Please try again shortly.").queue();
            return;
        }

        CommandRegistry registry = discordBot.getCommandRegistry();
        if (registry == null) {
            event.reply("No commands are registered at the moment.").queue();
            return;
        }

        List<SlashCommand> commands = registry.listRegisteredCommands();
        if (commands.isEmpty()) {
            event.reply("No commands are registered at the moment.").queue();
            return;
        }

        commands.sort(Comparator.comparing(SlashCommand::getName, String.CASE_INSENSITIVE_ORDER));
        StringBuilder descriptionBuilder = new StringBuilder();
        for (SlashCommand command : commands) {
            if (!descriptionBuilder.isEmpty()) {
                descriptionBuilder.append('\n');
            }

            String commandDescription = command.getDescription();
            if (commandDescription == null || commandDescription.isBlank()) {
                commandDescription = "No description provided.";
            }

            descriptionBuilder.append("`/")
                    .append(command.getName())
                    .append("` - ")
                    .append(commandDescription);
        }

        DiscordMessageSpec spec = DiscordMessageSpec.builder()
                .title("Available Commands")
                .description(descriptionBuilder.toString())
                .kind(DiscordMessageKind.INFO)
                .timestamp(true)
                .build();
        MessageCreateData payload = MessageFormatterManager.format(spec);

        event.reply(payload).queue();
    }
}
