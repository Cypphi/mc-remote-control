package dev.cypphi.mcrc.discord.command.commands;

import dev.cypphi.mcrc.MinecraftRemoteControl;
import dev.cypphi.mcrc.discord.command.SlashCommand;
import dev.cypphi.mcrc.discord.util.DiscordMessageKind;
import dev.cypphi.mcrc.discord.util.DiscordMessageSpec;
import dev.cypphi.mcrc.discord.util.MessageFormatterManager;
import dev.cypphi.mcrc.discord.util.chat.ChatLogUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import java.util.concurrent.CompletableFuture;

public class MessageCommand implements SlashCommand {
    private static final String OPTION_CONTENT = "content";

    @Override
    public String getName() {
        return "message";
    }

    @Override
    public String getDescription() {
        return "Send a raw chat message or command from the Minecraft client.";
    }

    @Override
    public CommandData asCommandData() {
        return Commands.slash(getName(), getDescription())
                .addOption(OptionType.STRING, OPTION_CONTENT, "Plain chat or command text to send.", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String content = getContent(event);
        if (content == null || content.isBlank()) {
            MessageCreateData payload = MessageFormatterManager.format(
                    DiscordMessageSpec.builder()
                            .description("Please provide the message or command you want to send.")
                            .kind(DiscordMessageKind.WARNING)
                            .build());
            event.reply(payload).queue();
            return;
        }

        MinecraftClient client = MinecraftRemoteControl.mc;
        if (client == null || client.player == null) {
            MessageCreateData payload = MessageFormatterManager.format(
                    DiscordMessageSpec.builder()
                            .description("Minecraft client is not ready yet or you're not in-game.")
                            .kind(DiscordMessageKind.ERROR)
                            .build());
            event.reply(payload).queue();
            return;
        }

        event.deferReply(false).queue(hook -> {
            CompletableFuture<SendResult> future = new CompletableFuture<>();
            client.execute(() -> {
                try {
                    ClientPlayNetworkHandler handler = client.getNetworkHandler();
                    if (handler == null) {
                        future.completeExceptionally(new IllegalStateException("Not connected to a server."));
                        return;
                    }

                    boolean hasCommandPrefix = content.startsWith("/") && content.length() > 1;
                    if (hasCommandPrefix) {
                        String command = content.substring(1);
                        handler.sendChatCommand(command);
                        future.complete(new SendResult(true, true));
                    } else {
                        handler.sendChatMessage(content);
                        future.complete(new SendResult(true, false));
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });

            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    MessageEditData payload = MessageEditData.fromCreateData(
                            MessageFormatterManager.format(
                                    DiscordMessageSpec.builder()
                                            .description("Failed to send message: " + throwable.getMessage())
                                            .kind(DiscordMessageKind.ERROR)
                                            .build()));
                    hook.editOriginal(payload).queue();
                    return;
                }

                if (result == null || !result.success()) {
                    MessageEditData payload = MessageEditData.fromCreateData(
                            MessageFormatterManager.format(
                                    DiscordMessageSpec.builder()
                                            .description("Failed to deliver the message.")
                                            .kind(DiscordMessageKind.WARNING)
                                            .build()));
                    hook.editOriginal(payload).queue();
                    return;
                }

                String description = result.command()
                        ? "Command sent successfully."
                        : "Message relayed to chat.";

                if (result.command()) {
                    ChatLogUtil.logOutgoing(content, true);
                }
                MessageEditData payload = MessageEditData.fromCreateData(
                        MessageFormatterManager.format(
                                DiscordMessageSpec.builder()
                                        .description(description)
                                        .kind(DiscordMessageKind.SUCCESS)
                                        .build()));
                hook.editOriginal(payload).queue();
            });
        });
    }

    private String getContent(SlashCommandInteractionEvent event) {
        OptionMapping option = event.getOption(OPTION_CONTENT);
        return option != null ? option.getAsString() : null;
    }

    private record SendResult(boolean success, boolean command) {}
}
