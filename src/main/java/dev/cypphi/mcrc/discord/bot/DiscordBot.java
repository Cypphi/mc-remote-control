package dev.cypphi.mcrc.discord.bot;

import dev.cypphi.mcrc.discord.command.CommandRegistry;
import dev.cypphi.mcrc.discord.command.SlashCommandListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public final class DiscordBot {
    private final String token;
    private final EnumSet<GatewayIntent> intents;
    private final List<Object> listeners;
    private final CommandRegistry commandRegistry;
    private final String commandGuildId;
    private JDA jda;

    DiscordBot(String token,
               EnumSet<GatewayIntent> intents,
               List<Object> listeners,
               CommandRegistry commandRegistry,
               String commandGuildId) {
        this.token = token;
        this.intents = intents.isEmpty() ? EnumSet.noneOf(GatewayIntent.class) : EnumSet.copyOf(intents);
        this.listeners = new ArrayList<>(listeners);
        this.commandRegistry = commandRegistry;
        this.commandGuildId = commandGuildId == null ? "" : commandGuildId.trim();
    }

    public JDA startAndAwaitReady() throws InterruptedException {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Discord bot token is missing.");
        }

        JDABuilder builder = JDABuilder.createDefault(token);
        if (!intents.isEmpty()) {
            builder.enableIntents(intents);
        }

        SlashCommandListener commandListener = new SlashCommandListener(commandRegistry);
        builder.addEventListeners(commandListener);
        listeners.forEach(builder::addEventListeners);

        try {
            jda = builder.build();
            jda.awaitReady();
        } catch (InvalidTokenException e) {
            throw e;
        }

        commandRegistry.syncCommands(jda, commandGuildId);
        return jda;
    }

    public void shutdownNow() {
        if (jda != null) {
            jda.shutdownNow();
        }
    }

    public Optional<JDA> getJda() {
        return Optional.ofNullable(jda);
    }

    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }
}
