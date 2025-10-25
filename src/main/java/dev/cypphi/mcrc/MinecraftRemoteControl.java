package dev.cypphi.mcrc;

import dev.cypphi.mcrc.config.MCRCConfig;
import dev.cypphi.mcrc.discord.bot.DiscordBot;
import dev.cypphi.mcrc.discord.bot.DiscordBotBuilder;
import dev.cypphi.mcrc.discord.command.CommandRegistry;
import dev.cypphi.mcrc.discord.command.commands.*;
import dev.cypphi.mcrc.discord.event.BotReadyListener;
import dev.cypphi.mcrc.discord.util.DiscordMessageKind;
import dev.cypphi.mcrc.discord.util.DiscordMessageSpec;
import dev.cypphi.mcrc.discord.util.DiscordMessageUtil;
import dev.cypphi.mcrc.remoteview.RemoteViewCoordinator;
import dev.cypphi.mcrc.remoteview.RemoteViewSessionManager;
import dev.cypphi.mcrc.remoteview.capture.RemoteViewCaptureService;
import dev.cypphi.mcrc.remoteview.signaling.RemoteViewSignalingServer;
import dev.cypphi.mcrc.remoteview.stream.MjpegRemoteViewPublisher;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftRemoteControl implements ClientModInitializer {
	public static final String MOD_VERSION = FabricLoader.getInstance()
			.getModContainer(MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("dev");
	public static final String MOD_VERSION = "1.0.0";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID.toUpperCase());
	public static final MinecraftClient mc = MinecraftClient.getInstance();

	private static JDA jda;
	private static DiscordBot discordBot;
	private static final RemoteViewSessionManager remoteViewSessionManager = new RemoteViewSessionManager();
	private static final RemoteViewCaptureService remoteViewCaptureService = new RemoteViewCaptureService();
	private static final MjpegRemoteViewPublisher remoteViewStreamPublisher = new MjpegRemoteViewPublisher();
	private static final RemoteViewCoordinator remoteViewCoordinator =
			new RemoteViewCoordinator(remoteViewCaptureService, remoteViewStreamPublisher, () -> MCRCConfig.HANDLER.instance());
	private static RemoteViewSignalingServer remoteViewSignalingServer;

	static {
		remoteViewSessionManager.setLifecycleListener(remoteViewCoordinator);
	}

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing {} {}...", MOD_ID, MOD_VERSION);

		MCRCConfig.HANDLER.load();

		MCRCConfig config = MCRCConfig.HANDLER.instance();
		remoteViewSessionManager.setSessionTtlSeconds(config.remoteViewLinkTimeoutSeconds);

			if (config.remoteViewEnabled) {
				ensureSignalingServer(config);
			}

			if (config.autoStart) {
				if (config.botToken == null || config.botToken.isBlank()) {
					LOGGER.warn("Discord bot token is not configured; skipping bot startup.");
					return;
		}

			try {
                CommandRegistry commandRegistry = new CommandRegistry()
                        .register(new MessageCommand())
                        .register(new RemoteViewCommand())
                        .register(new ScreenshotCommand())
                        .register(new PingCommand());

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
                DiscordMessageUtil.sendMessage(
                        DiscordMessageSpec.builder()
                                .title("MC Remote Control")
                                .description("Discord bot is online.")
                                .kind(DiscordMessageKind.SUCCESS)
                                .timestamp(true)
                                .build());
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

	public static RemoteViewSessionManager getRemoteViewSessionManager() {
		return remoteViewSessionManager;
	}

	public static RemoteViewCaptureService getRemoteViewCaptureService() {
		return remoteViewCaptureService;
	}

	public static RemoteViewCoordinator getRemoteViewCoordinator() {
		return remoteViewCoordinator;
	}

	private static void ensureSignalingServer(MCRCConfig config) {
        if (remoteViewSignalingServer == null) {
            remoteViewSignalingServer = new RemoteViewSignalingServer(remoteViewSessionManager, remoteViewStreamPublisher);
        }

		if (remoteViewSignalingServer.getBoundAddress().isPresent()) {
			return;
		}

		try {
			remoteViewSignalingServer.start(config.remoteViewBindAddress, config.remoteViewPort);
		} catch (Exception e) {
			LOGGER.error("Failed to start Remote View signaling server: {}", e.getMessage());
		}
	}
}
