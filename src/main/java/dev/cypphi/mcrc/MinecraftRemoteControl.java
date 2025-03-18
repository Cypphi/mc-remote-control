package dev.cypphi.mcrc;

import dev.cypphi.mcrc.screen.JDADownloadPromptScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

public class MinecraftRemoteControl implements ClientModInitializer {
	public static final String MOD_ID = "mcrc";
	public static final String MOD_VERSION = "0.0.1";
	public static final Path MOD_DIR = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID.toUpperCase());

	private static final String JDA_VERSION = "5.3.0";
	private static final String JDA_URL = "https://repo1.maven.org/maven2/net/dv8tion/JDA/" + JDA_VERSION + "/JDA-" + JDA_VERSION + ".jar";
	private static final Path JDA_LIB_DIR = MOD_DIR.resolve("libraries");
	private static final Path JDA_JAR_PATH = JDA_LIB_DIR.resolve("JDA-" + JDA_VERSION + ".jar");

	private static boolean hasPrompted = false;

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing {} {}...", MOD_ID, MOD_VERSION);
		ensureDirectoriesExist();
	}

	public static void onTitleScreenOpened() {
		if (!isJDAPresent() && !hasPrompted) {
			LOGGER.info("JDA is missing or outdated. Showing prompt...");
			MinecraftClient.getInstance().setScreen(new JDADownloadPromptScreen(MinecraftClient.getInstance().currentScreen));
		} else {
			LOGGER.info("JDA is already installed. Loading...");
			new Thread(MinecraftRemoteControl::loadJDA).start();
		}
	}

	private void ensureDirectoriesExist() {
		try {
			if (!Files.exists(JDA_LIB_DIR)) {
				Files.createDirectories(JDA_LIB_DIR);
				LOGGER.info("Created directory: {}", JDA_LIB_DIR);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to create directories!", e);
		}
	}

	private static boolean isJDAPresent() {
		return Files.exists(JDA_JAR_PATH) && isValidJar(JDA_JAR_PATH.toFile());
	}

	public static void startJDADownload() {
		new Thread(() -> {
			try {
				new MinecraftRemoteControl().downloadJDA();
			} catch (Exception e) {
				LOGGER.error("Failed to download JDA!", e);
			}
		}).start();
	}

	private void downloadJDA() {
		try {
			LOGGER.info("Downloading JDA...");
			URL url = new URI(JDA_URL).toURL();
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");

			try (InputStream in = connection.getInputStream();
				 FileOutputStream out = new FileOutputStream(JDA_JAR_PATH.toFile())) {

				byte[] buffer = new byte[4096];
				int bytesRead;

				while ((bytesRead = in.read(buffer)) != -1) {
					out.write(buffer, 0, bytesRead);
				}
			}

			LOGGER.info("JDA download complete.");
			loadJDA();

		} catch (IOException | URISyntaxException e) {
			LOGGER.error("Failed to download JDA!", e);
		}
	}

	private static boolean isValidJar(File file) {
		try (JarFile jarFile = new JarFile(file)) {
			return jarFile.entries().hasMoreElements();
		} catch (IOException e) {
			return false;
		}
	}

	private static void loadJDA() {
		try {
			URLClassLoader classLoader = new URLClassLoader(new URL[]{JDA_JAR_PATH.toUri().toURL()}, MinecraftRemoteControl.class.getClassLoader());
			Thread.currentThread().setContextClassLoader(classLoader);
			showToast("Discord JDA was successfully loaded.");
			LOGGER.info("JDA loaded successfully.");
		} catch (Exception e) {
			LOGGER.error("Failed to load JDA!", e);
		}
	}

	private static void showToast(String message) {
		MinecraftClient client = MinecraftClient.getInstance();
		ToastManager toastManager = client.getToastManager();
		SystemToast toast = SystemToast.create(client, SystemToast.Type.PERIODIC_NOTIFICATION, Text.of("Remote Control"), Text.of(message));
		toastManager.add(toast);
	}

	public static void setHasPrompted() {
		hasPrompted = true;
	}

	public static boolean getHasPrompted() {
		return hasPrompted;
	}
}
