package dev.cypphi.mcrc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;

public class MinecraftRemoteControl implements ClientModInitializer {
	public static final String MOD_ID = "mcrc";
	public static final String MOD_VERSION = "1.0.0";
	public static final Path MOD_DIR = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID.toUpperCase());

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing {} {}...", MOD_ID, MOD_VERSION);
	}
}
