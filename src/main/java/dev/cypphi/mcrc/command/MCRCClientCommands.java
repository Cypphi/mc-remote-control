package dev.cypphi.mcrc.command;

import com.mojang.brigadier.Command;
import dev.cypphi.mcrc.MinecraftRemoteControl;
import dev.cypphi.mcrc.MinecraftRemoteControl.BotStartResult;
import dev.cypphi.mcrc.MinecraftRemoteControl.BotStartStatus;
import dev.cypphi.mcrc.MinecraftRemoteControl.BotStopResult;
import dev.cypphi.mcrc.MinecraftRemoteControl.BotStopStatus;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;

public final class MCRCClientCommands {
    private MCRCClientCommands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal(MinecraftRemoteControl.MOD_ID)
                        .then(ClientCommandManager.literal("start").executes(ctx -> handleStart(ctx.getSource())))
                        .then(ClientCommandManager.literal("stop").executes(ctx -> handleStop(ctx.getSource())))));
    }

    private static int handleStart(FabricClientCommandSource source) {
        if (MinecraftRemoteControl.isBotRunning()) {
            sendFeedback(source, Formatting.YELLOW, "Discord bot is already running.");
            return 0;
        }

        sendFeedback(source, Formatting.GRAY, "Starting Discord bot...");
        CompletableFuture
                .supplyAsync(MinecraftRemoteControl::startDiscordBot)
                .whenComplete((result, throwable) ->
                        runOnClientThread(() -> processStartResult(source, result, throwable)));
        return Command.SINGLE_SUCCESS;
    }

    private static void processStartResult(FabricClientCommandSource source, BotStartResult result, Throwable throwable) {
        if (throwable != null) {
            MinecraftRemoteControl.LOGGER.error("Failed to start Discord bot via command", throwable);
            sendFeedback(source, Formatting.RED, "Failed to start Discord bot: " + nullSafe(throwable.getMessage()));
            return;
        }

        if (result == null) {
            sendFeedback(source, Formatting.RED, "Failed to start Discord bot: unknown error");
            return;
        }

        BotStartStatus status = result.status();
        switch (status) {
            case SUCCESS -> sendFeedback(source, Formatting.GREEN, "Discord bot is online.");
            case ALREADY_RUNNING -> sendFeedback(source, Formatting.YELLOW, "Discord bot is already running.");
            case MISSING_TOKEN -> sendFeedback(source, Formatting.RED, "Discord bot token is not configured.");
            case INVALID_TOKEN -> sendFeedback(source, Formatting.RED, "Invalid Discord bot token.");
            case INTERRUPTED -> sendFeedback(source, Formatting.RED, "Discord bot startup interrupted.");
            case ERROR -> sendFeedback(source, Formatting.RED, "Failed to start Discord bot: " + nullSafe(result.errorMessage()));
        }
    }

    private static int handleStop(FabricClientCommandSource source) {
        BotStopResult result = MinecraftRemoteControl.stopDiscordBot();
        BotStopStatus status = result.status();
        switch (status) {
            case STOPPED -> {
                sendFeedback(source, Formatting.GRAY, "Discord bot is shutting down.");
                return Command.SINGLE_SUCCESS;
            }
            case NOT_RUNNING -> {
                sendFeedback(source, Formatting.YELLOW, "Discord bot is not running.");
                return 0;
            }
            case ERROR -> {
                sendFeedback(source, Formatting.RED, "Failed to stop Discord bot: " + nullSafe(result.errorMessage()));
                return 0;
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static MutableText prefixed(String message, Formatting colour) {
        String label = MinecraftRemoteControl.MOD_ID.toUpperCase();
        MutableText prefix = Text.empty()
                .append(Text.literal("[").formatted(Formatting.GRAY))
                .append(Text.literal(label).formatted(Formatting.AQUA))
                .append(Text.literal("] ").formatted(Formatting.GRAY));
        return prefix.append(Text.literal(message).formatted(colour));
    }

    private static String nullSafe(String value) {
        return value == null || value.isBlank() ? "unknown error" : value;
    }

    private static void sendFeedback(FabricClientCommandSource source, Formatting colour, String message) {
        source.sendFeedback(prefixed(message, colour));
    }

    private static void runOnClientThread(Runnable task) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.execute(task);
        } else {
            task.run();
        }
    }
}
