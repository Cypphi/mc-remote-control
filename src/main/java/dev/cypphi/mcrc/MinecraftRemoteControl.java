package dev.cypphi.mcrc;

import dev.cypphi.mcrc.command.MCRCClientCommands;
import dev.cypphi.mcrc.config.MCRCConfig;
import dev.cypphi.mcrc.discord.bot.DiscordBot;
import dev.cypphi.mcrc.discord.bot.DiscordBotBuilder;
import dev.cypphi.mcrc.discord.command.CommandRegistry;
import dev.cypphi.mcrc.discord.command.commands.*;
import dev.cypphi.mcrc.discord.event.BotReadyListener;
import dev.cypphi.mcrc.discord.notification.SessionNotificationManager;
import dev.cypphi.mcrc.util.discord.DiscordMessageKind;
import dev.cypphi.mcrc.util.discord.DiscordMessageSpec;
import dev.cypphi.mcrc.util.discord.DiscordMessageUtil;
import dev.cypphi.mcrc.remoteview.RemoteViewCoordinator;
import dev.cypphi.mcrc.remoteview.RemoteViewSessionManager;
import dev.cypphi.mcrc.remoteview.capture.RemoteViewCaptureService;
import dev.cypphi.mcrc.remoteview.signaling.RemoteViewSignalingServer;
import dev.cypphi.mcrc.remoteview.stream.MjpegRemoteViewPublisher;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftRemoteControl implements ClientModInitializer {
	public static final String MOD_ID = "mcrc";
	public static final String MOD_VERSION = FabricLoader.getInstance()
			.getModContainer(MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("dev");
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

    public enum BotStartStatus {
        SUCCESS,
        ALREADY_RUNNING,
        MISSING_TOKEN,
        INVALID_TOKEN,
        INTERRUPTED,
        ERROR
    }

    public enum BotStopStatus {
        STOPPED,
        NOT_RUNNING,
        ERROR
    }

    public record BotStartResult(BotStartStatus status, String errorMessage) {}

    public record BotStopResult(BotStopStatus status, String errorMessage) {}

	static {
		remoteViewSessionManager.setLifecycleListener(remoteViewCoordinator);
	}

	@Override
	public void onInitializeClient() {
        if (!MOD_VERSION.equals("unknown")) {
            LOGGER.info("Initializing {} {}...", MOD_ID.toUpperCase(), MOD_VERSION);
        } else {
            LOGGER.info("Initializing {} version unknown...", MOD_ID.toUpperCase());
        }

		MCRCConfig.HANDLER.load();

		MCRCConfig config = MCRCConfig.HANDLER.instance();
		remoteViewSessionManager.setSessionTtlSeconds(config.remoteViewLinkTimeoutSeconds);
        SessionNotificationManager.init();
        MCRCClientCommands.register();

        if (config.remoteViewEnabled) {
            ensureSignalingServer(config);
        }

        if (config.autoStart) {
            startDiscordBot();
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

    public static synchronized BotStartResult startDiscordBot() {
        if (isBotRunning()) {
            return new BotStartResult(BotStartStatus.ALREADY_RUNNING, null);
        }

        MCRCConfig config = MCRCConfig.HANDLER.instance();
        String token = config.botToken == null ? "" : config.botToken.trim();
        if (token.isEmpty()) {
            LOGGER.warn("Discord bot token is not configured; cannot start bot.");
            return new BotStartResult(BotStartStatus.MISSING_TOKEN, null);
        }

        String guildId = config.commandGuildId == null ? "" : config.commandGuildId.trim();

        CommandRegistry commandRegistry = createCommandRegistry();

        try {
            DiscordBot newBot = new DiscordBotBuilder()
                    .withToken(token)
                    .addIntent(GatewayIntent.GUILD_MESSAGES)
                    .addIntent(GatewayIntent.MESSAGE_CONTENT)
                    .withCommandGuildId(guildId)
                    .withCommandRegistry(commandRegistry)
                    .addEventListener(new BotReadyListener())
                    .build();

            JDA newJda = newBot.startAndAwaitReady();

            discordBot = newBot;
            jda = newJda;

            LOGGER.info("Discord bot initialized successfully");
            if (config.notifyOnBotReady) {
                DiscordMessageUtil.sendMessage(
                        DiscordMessageSpec.builder()
                                .title("MC Remote Control")
                                .description("Discord bot is online.")
                                .kind(DiscordMessageKind.SUCCESS)
                                .timestamp(true)
                                .build());
            }

            return new BotStartResult(BotStartStatus.SUCCESS, null);
        } catch (InvalidTokenException e) {
            LOGGER.error("Invalid token provided. Please enter a valid token.");
            return new BotStartResult(BotStartStatus.INVALID_TOKEN, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Discord bot startup interrupted.");
            return new BotStartResult(BotStartStatus.INTERRUPTED, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error while starting Discord bot: {}", e.getMessage());
            return new BotStartResult(BotStartStatus.ERROR, e.getMessage());
        }
    }

    public static synchronized BotStopResult stopDiscordBot() {
        if (!isBotRunning()) {
            return new BotStopResult(BotStopStatus.NOT_RUNNING, null);
        }

        try {
            if (discordBot != null) {
                discordBot.shutdownNow();
            } else if (jda != null) {
                jda.shutdownNow();
            }
            LOGGER.info("Discord bot shut down.");
        } catch (Exception e) {
            LOGGER.warn("Encountered an error while shutting down Discord bot: {}", e.getMessage(), e);
            return new BotStopResult(BotStopStatus.ERROR, e.getMessage());
        }

        discordBot = null;
        jda = null;
        return new BotStopResult(BotStopStatus.STOPPED, null);
    }

    public static synchronized boolean isBotRunning() {
        if (jda == null) {
            return false;
        }
        JDA.Status status = jda.getStatus();
        return status != JDA.Status.SHUTTING_DOWN && status != JDA.Status.SHUTDOWN;
    }

    private static CommandRegistry createCommandRegistry() {
        return new CommandRegistry()
                .register(new HelpCommand())
                .register(new MessageCommand())
                .register(new PingCommand())
                .register(new RemoteViewCommand())
                .register(new ScreenshotCommand());
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
