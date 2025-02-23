package dev.cypphi.mcrc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.cypphi.mcrc.MinecraftRemoteControl;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "minecraftremotecontrol.json");

    private static Config instance = new Config();

    private String discordBotToken = "";
    private String discordChannelID = "";

    public static Config getInstance() {
        return instance;
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            MinecraftRemoteControl.LOGGER.info("Config file not found, creating default config.");
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            Config loadedInstance = GSON.fromJson(reader, Config.class);
            if (loadedInstance != null) {
                instance = loadedInstance;
                MinecraftRemoteControl.LOGGER.info("Config loaded successfully.");
            } else {
                MinecraftRemoteControl.LOGGER.warn("Loaded config was null, resetting to default.");
                instance = new Config();
                save();
            }
        } catch (IOException e) {
            MinecraftRemoteControl.LOGGER.error("Error loading config: " + e.getMessage());
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
            MinecraftRemoteControl.LOGGER.info("Config saved successfully.");
        } catch (IOException e) {
            MinecraftRemoteControl.LOGGER.error("Error saving config: " + e.getMessage());
        }
    }

    public String getDiscordBotToken() {
        return discordBotToken;
    }

    public String getDiscordChannelID() {
        return discordChannelID;
    }

    public void setDiscordBotToken(String token) {
        this.discordBotToken = token;
        save();
    }

    public void setDiscordChannelID(String channelID) {
        this.discordChannelID = channelID;
        save();
    }
}