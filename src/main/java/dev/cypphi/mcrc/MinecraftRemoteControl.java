package dev.cypphi.mcrc;

import dev.cypphi.mcrc.config.MCRCConfig;
import dev.cypphi.mcrc.discord.bot.DiscordBot;
import dev.cypphi.mcrc.discord.bot.DiscordBotBuilder;
import dev.cypphi.mcrc.discord.command.CommandRegistry;
import dev.cypphi.mcrc.discord.command.commands.TestCommand;
import dev.cypphi.mcrc.discord.command.commands.RemoteViewCommand;
import dev.cypphi.mcrc.discord.event.BotReadyListener;
import dev.cypphi.mcrc.discord.util.DiscordMessageUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftRemoteControl implements ClientModInitializer {
	public static final String MOD_ID = "mcrc";
	public static final String MOD_VERSION = "1.0.0";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID.toUpperCase());
	public static final MinecraftClient mc = MinecraftClient.getInstance();

	private static JDA jda;
	private static DiscordBot discordBot;

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing {} {}...", MOD_ID, MOD_VERSION);

		MCRCConfig.HANDLER.load();

		MCRCConfig config = MCRCConfig.HANDLER.instance();

		if (config.autoStart) {
			if (config.botToken == null || config.botToken.isBlank()) {
				LOGGER.warn("Discord bot token is not configured; skipping bot startup.");
				return;
			}

			try {
				CommandRegistry commandRegistry = new CommandRegistry()
						.register(new TestCommand())
						.register(new RemoteViewCommand());

				discordBot = new DiscordBotBuilder()
						.withToken(config.botToken)
						.addIntent(GatewayIntent.GUILD_MESSAGES)
						.addIntent(GatewayIntent.MESSAGE_CONTENT)
						.withCommandGuildId(config.commandGuildId)
						.withCommandRegistry(commandRegistry)
						.addEventListener(new BotReadyListener())
						.build();

				jda = discordBot.startAndAwaitReady();
				LOGGER.info("Discord bot initialized successfully");
				DiscordMessageUtil.sendMessage("Discord bot is online.", "ready");
			} catch (InvalidTokenException e) {
				LOGGER.error("Invalid token provided. Please enter a valid token.");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				LOGGER.error("Discord bot startup interrupted.");
			} catch (Exception e) {
				LOGGER.error("Unexpected error: {}", e.getMessage());
			}
		}
	}

	public static JDA getJDA() {
		return jda;
	}

	public static DiscordBot getDiscordBot() {
		return discordBot;
	}
}
