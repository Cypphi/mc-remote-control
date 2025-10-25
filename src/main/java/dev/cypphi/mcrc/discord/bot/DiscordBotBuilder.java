package dev.cypphi.mcrc.discord.bot;

import dev.cypphi.mcrc.discord.command.CommandRegistry;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public final class DiscordBotBuilder {
    private String token;
    private final EnumSet<GatewayIntent> intents = EnumSet.noneOf(GatewayIntent.class);
    private final List<Object> listeners = new ArrayList<>();
    private CommandRegistry commandRegistry = new CommandRegistry();
    private String commandGuildId = "";

    public DiscordBotBuilder withToken(String token) {
        this.token = token;
        return this;
    }

    public DiscordBotBuilder addIntent(GatewayIntent intent) {
        this.intents.add(intent);
        return this;
    }

    public DiscordBotBuilder addIntents(Collection<GatewayIntent> intents) {
        this.intents.addAll(intents);
        return this;
    }

    public DiscordBotBuilder addEventListener(Object listener) {
        this.listeners.add(listener);
        return this;
    }

    public DiscordBotBuilder withCommandRegistry(CommandRegistry registry) {
        this.commandRegistry = registry;
        return this;
    }

    public DiscordBotBuilder withCommandGuildId(String guildId) {
        this.commandGuildId = guildId == null ? "" : guildId;
        return this;
    }

    public DiscordBot build() {
        return new DiscordBot(token, intents, listeners, commandRegistry, commandGuildId);
    }
}
