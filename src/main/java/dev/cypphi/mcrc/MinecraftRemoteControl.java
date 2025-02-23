package dev.cypphi.mcrc;

import dev.cypphi.mcrc.config.Config;
import dev.cypphi.mcrc.discord.event.DiscordEventListener;
import dev.cypphi.mcrc.discord.event.JDAEventListener;
import dev.cypphi.mcrc.discord.utils.DiscordMessageUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftRemoteControl implements ClientModInitializer {
	public static final String MOD_ID = "mcrc";
	public static final String MOD_VERSION = "0.0.1";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID.toUpperCase());

	private static JDA jda;

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing {} {}...", MOD_ID, MOD_VERSION);

		Config.load();

		try {
			jda = JDABuilder.createDefault(Config.getInstance().getDiscordBotToken(), GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
					.build()
					.awaitReady();

			jda.addEventListener(new JDAEventListener(jda));
			jda.addEventListener(new DiscordEventListener());

			LOGGER.info("Discord bot initialized successfully");

			DiscordMessageUtils.sendToDiscord("Discord bot is online.");
		} catch (Exception e) {
			LOGGER.error("Failed to initialize Discord bot.");
		}
	}

	public static JDA getJDA() {
		return jda;
	}
}