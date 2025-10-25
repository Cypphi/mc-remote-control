package dev.cypphi.mcrc.discord.command;

import dev.cypphi.mcrc.MinecraftRemoteControl;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CommandRegistry {
    private final Map<String, SlashCommand> commands = new LinkedHashMap<>();

    public CommandRegistry register(SlashCommand command) {
        Objects.requireNonNull(command, "command");
        commands.put(command.getName(), command);
        return this;
    }

    public void handle(SlashCommandInteractionEvent event) {
        SlashCommand command = commands.get(event.getName());
        if (command == null) {
            event.reply("Unknown command.").setEphemeral(true).queue();
            return;
        }

        command.execute(event);
    }

    public List<CommandData> buildCommandData() {
        List<CommandData> data = new ArrayList<>();
        commands.values().forEach(command -> data.add(command.asCommandData()));
        return data;
    }

    public void syncCommands(JDA jda, String guildId) {
        List<CommandData> data = buildCommandData();
        if (data.isEmpty()) {
            return;
        }

        if (guildId != null && !guildId.isBlank()) {
            Guild guild = jda.getGuildById(guildId.trim());
            if (guild == null) {
                MinecraftRemoteControl.LOGGER.warn("Failed to sync slash commands: guild {} not found.", guildId);
                return;
            }

            guild.updateCommands().addCommands(data).queue(
                    unused -> MinecraftRemoteControl.LOGGER.info("Registered {} slash command(s) to guild {}.", data.size(), guildId),
                    error -> MinecraftRemoteControl.LOGGER.error("Failed to register guild slash commands: {}", error.getMessage())
            );
            return;
        }

        jda.updateCommands().addCommands(data).queue(
                unused -> MinecraftRemoteControl.LOGGER.info("Registered {} global slash command(s).", data.size()),
                error -> MinecraftRemoteControl.LOGGER.error("Failed to register global slash commands: {}", error.getMessage())
        );
    }
}
