package dev.cypphi.mcrc.discord.notification;

import dev.cypphi.mcrc.config.MCRCConfig;
import dev.cypphi.mcrc.util.discord.DiscordMessageKind;
import dev.cypphi.mcrc.util.discord.DiscordMessageSpec;
import dev.cypphi.mcrc.util.discord.DiscordMessageUtil;
import dev.cypphi.mcrc.util.discord.DiscordPingUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.awt.Color;

public final class SessionNotificationManager {
    private static boolean initialized;

    private static final Color DISCONNECT_COLOR = new Color(0xFF6B6B);

    private static boolean singleplayerActive;
    private static boolean inMultiplayerSession;
    private static String lastServerName;
    private static String lastServerAddress;
    private static boolean lastServerWasRealms;

    private static volatile boolean serverKickPending;
    private static volatile String serverKickReason;

    private SessionNotificationManager() {}

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ClientTickEvents.END_CLIENT_TICK.register(SessionNotificationManager::handleClientTick);

        ClientPlayConnectionEvents.JOIN.register(SessionNotificationManager::handleMultiplayerJoin);
        ClientPlayConnectionEvents.DISCONNECT.register(SessionNotificationManager::handleMultiplayerDisconnect);
    }

    public static void handleServerDisconnect(Text reason) {
        serverKickPending = true;
        serverKickReason = reason != null ? reason.getString() : null;
    }

    private static void handleClientTick(MinecraftClient client) {
        boolean currentlySingleplayer = client.isIntegratedServerRunning() && client.world != null;
        if (!singleplayerActive && currentlySingleplayer) {
            client.execute(SessionNotificationManager::sendSingleplayerJoin);
        } else if (singleplayerActive && !currentlySingleplayer) {
            client.execute(SessionNotificationManager::sendSingleplayerDisconnect);
        }
        singleplayerActive = currentlySingleplayer;
    }

    private static void handleMultiplayerJoin(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
        if (handler.getConnection().isLocal()) {
            resetServerDisconnectState();
            return;
        }

        inMultiplayerSession = true;
        ServerInfo info = client.getCurrentServerEntry();
        lastServerName = info != null ? info.name : null;
        lastServerAddress = info != null ? info.address : null;
        lastServerWasRealms = info != null && info.isRealm();

        resetServerDisconnectState();

        MCRCConfig config = MCRCConfig.HANDLER.instance();
        if (!config.notifyOnJoinMultiplayer) {
            return;
        }

        final String serverName = lastServerName;
        final String serverAddress = lastServerAddress;
        final boolean serverWasRealms = lastServerWasRealms;

        client.execute(() -> {
            DiscordMessageSpec.Builder builder = DiscordMessageSpec.builder()
                    .title("Session Update")
                    .description("Connected to multiplayer server.")
                    .kind(DiscordMessageKind.SUCCESS)
                    .timestamp(true);

            appendServerFields(builder, serverName, serverAddress, serverWasRealms);
            DiscordMessageUtil.sendMessage(builder.build());
        });
    }

    private static void handleMultiplayerDisconnect(ClientPlayNetworkHandler handler, MinecraftClient client) {
        if (!inMultiplayerSession || handler.getConnection().isLocal()) {
            resetMultiplayerState();
            return;
        }

        final boolean kicked = serverKickPending;
        final String reason = serverKickReason;

        MCRCConfig config = MCRCConfig.HANDLER.instance();
        final String serverName = lastServerName;
        final String serverAddress = lastServerAddress;
        final boolean serverWasRealms = lastServerWasRealms;

        if (config.notifyOnDisconnectMultiplayer) {
            client.execute(() -> {
                boolean shouldPing = kicked && config.pingOnDisconnectMultiplayer;

                String description = (kicked
                        ? "Kicked from multiplayer server."
                        : "Disconnected from multiplayer server.");

                DiscordMessageSpec.Builder builder = DiscordMessageSpec.builder()
                        .title("Session Update")
                        .description(description)
                        .kind(kicked ? DiscordMessageKind.WARNING : DiscordMessageKind.INFO)
                        .timestamp(true)
                        .colorOverride(DISCONNECT_COLOR);

                DiscordPingUtil.applyAllowedUserMention(builder, shouldPing, kicked ? "multiplayer kick" : "multiplayer disconnect");

                appendServerFields(builder, serverName, serverAddress, serverWasRealms);

                if (kicked && reason != null && !reason.isBlank()) {
                    builder.addField("Reason", reason, false);
                }

                DiscordMessageUtil.sendMessage(builder.build());
            });
        }

        resetMultiplayerState();
    }

    private static void sendSingleplayerJoin() {
        MCRCConfig config = MCRCConfig.HANDLER.instance();
        if (!config.notifyOnJoinSingleplayer) {
            return;
        }

        DiscordMessageSpec spec = DiscordMessageSpec.builder()
                .title("Session Update")
                .description("Singleplayer world loaded.")
                .kind(DiscordMessageKind.SUCCESS)
                .timestamp(true)
                .build();
        DiscordMessageUtil.sendMessage(spec);
    }

    private static void sendSingleplayerDisconnect() {
        MCRCConfig config = MCRCConfig.HANDLER.instance();
        if (!config.notifyOnDisconnectSingleplayer) {
            return;
        }

        DiscordMessageSpec spec = DiscordMessageSpec.builder()
                .title("Session Update")
                .description("Singleplayer world closed.")
                .kind(DiscordMessageKind.INFO)
                .timestamp(true)
                .colorOverride(DISCONNECT_COLOR)
                .build();
        DiscordMessageUtil.sendMessage(spec);
    }

    private static void appendServerFields(DiscordMessageSpec.Builder builder, String serverName, String serverAddress, boolean serverWasRealms) {
        if (serverName != null && !serverName.isBlank()) {
            builder.addField("Server", serverName, true);
        } else if (serverWasRealms) {
            builder.addField("Server", "Minecraft Realms", true);
        }

        if (serverAddress != null && !serverAddress.isBlank()) {
            builder.addField("Address", serverAddress, true);
        }
    }

    private static void resetServerDisconnectState() {
        serverKickPending = false;
        serverKickReason = null;
    }

    private static void resetMultiplayerState() {
        inMultiplayerSession = false;
        lastServerName = null;
        lastServerAddress = null;
        lastServerWasRealms = false;
        resetServerDisconnectState();
    }
}
