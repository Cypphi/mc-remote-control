package dev.cypphi.mcrc.discord.command.commands;

import dev.cypphi.mcrc.discord.command.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class PingCommand implements SlashCommand {
    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public String getDescription() {
        return "Checks the bot latency.";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        long gatewayPing = event.getJDA().getGatewayPing();
        event.reply("Pong! Current latency: **" + gatewayPing + "ms**").queue();
    }
}
