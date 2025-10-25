package dev.cypphi.mcrc.discord.command.commands;

import dev.cypphi.mcrc.discord.command.SlashCommand;
import dev.cypphi.mcrc.discord.util.DiscordMessageKind;
import dev.cypphi.mcrc.discord.util.DiscordMessageSpec;
import dev.cypphi.mcrc.discord.util.MessageFormatterManager;
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
        DiscordMessageSpec spec = DiscordMessageSpec.builder()
                .title("Pong!")
                .description("Current gateway latency: **" + gatewayPing + " ms**")
                .kind(DiscordMessageKind.INFO)
                .timestamp(true)
                .build();
        event.reply(MessageFormatterManager.format(spec)).queue();
    }
}
