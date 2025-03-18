package dev.cypphi.mcrc;

import dev.cypphi.mcrc.config.MCRCConfig;
import dev.cypphi.mcrc.discord.event.DiscordEventListener;
import dev.cypphi.mcrc.discord.event.JDAEventListener;
import dev.cypphi.mcrc.discord.util.DiscordMessageUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftRemoteControl implements ClientModInitializer {
	public static final String MOD_ID = "mcrc";
	public static final String MOD_VERSION = "1.0.0";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID.toUpperCase());

	private static JDA jda;

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing {} {}...", MOD_ID, MOD_VERSION);

		MCRCConfig.HANDLER.load();

		if (MCRCConfig.HANDLER.instance().autoStart) {
			try {
				jda = JDABuilder.createDefault(MCRCConfig.HANDLER.instance().botToken, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
						.build()
						.awaitReady();

//				jda.addEventListener(new JDAEventListener(jda));
				jda.addEventListener(new DiscordEventListener());

				LOGGER.info("Discord bot initialized successfully");

				DiscordMessageUtil.sendMessage("Discord bot is online.");
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static JDA getJDA() {
		return jda;
	}
}
